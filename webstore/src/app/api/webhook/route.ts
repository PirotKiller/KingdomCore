export const dynamic = "force-dynamic";

import Stripe from "stripe";
import { NextRequest, NextResponse } from "next/server";
import { getStripe } from "@/lib/stripe";
import dbConnect from "@/lib/mongodb";
import { Purchase } from "@/models/Purchase";
import { StoreItem } from "@/models/StoreItem";
import mongoose from "mongoose";
import { randomBytes } from "crypto";

const generateTransactionId = () => {
  const chars = randomBytes(4).toString("hex").toUpperCase();
  return `KC-${chars.slice(0, 4)}-${chars.slice(4, 8)}`;
};

export async function POST(req: NextRequest) {
  const body = await req.text();
  const sig = req.headers.get("stripe-signature");

  if (!sig) {
    console.error("Webhook Error: No stripe-signature header");
    return NextResponse.json({ error: "No signature" }, { status: 400 });
  }

  let event: Stripe.Event;

  try {
    const stripe = getStripe();
    event = stripe.webhooks.constructEvent(body, sig, process.env.STRIPE_WEBHOOK_SECRET!);
    console.log("✅ Webhook verified:", event.id, event.type);
  } catch (err: any) {
    console.error("❌ Webhook Signature Error:", err.message);
    return NextResponse.json({ error: `Webhook Error: ${err.message}` }, { status: 400 });
  }

    if (event.type === "checkout.session.completed") {
        const session = event.data.object as Stripe.Checkout.Session;
        const meta = session.metadata;

        if (!meta || !meta.userId || !meta.itemId || !meta.minecraftUuid) {
            console.warn("⚠️ Webhook Warning: Incomplete metadata in session:", session.id, meta);
            return NextResponse.json({ error: "Missing metadata" }, { status: 200 });
        }

        try {
            console.log("🔄 Processing purchase for session:", session.id);
            await dbConnect();

            // Register models
            if (!mongoose.models.Purchase) {
                console.log("📦 Registering Purchase model...");
            }

            // Record Purchase
            const purchaseData = {
                userId: new mongoose.Types.ObjectId(meta.userId),
                discordId: meta.discordId,
                minecraftUuid: meta.minecraftUuid,
                itemId: new mongoose.Types.ObjectId(meta.itemId),
                itemName: meta.itemName,
                price: session.amount_total || 0,
                currency: session.currency || "usd",
                stripeSessionId: session.id,
                transactionId: generateTransactionId(),
                status: "completed",
            };

            const purchase = await Purchase.create(purchaseData);
            console.log("✅ Purchase record created:", purchase._id);

            // --- LOGGING: PURCHASE COMPLETE ---
            try {
                const { recordWebLog } = await import("@/lib/logger");
                await recordWebLog({
                    type: "STORE_PURCHASE_COMPLETE",
                    executor: { discordId: "system", name: "SYSTEM (Stripe)" },
                    summary: `Payment received for ${meta.itemName} from ${meta.minecraftUsername || meta.minecraftUuid} ($${((session.amount_total || 0) / 100).toFixed(2)})`,
                    metadata: { 
                        purchaseId: purchase._id, 
                        stripeSessionId: session.id,
                        minecraftUuid: meta.minecraftUuid,
                        amount: session.amount_total
                    }
                });
            } catch (le) { console.error("Log error:", le); }

            // Delivery logic
            const item = await StoreItem.findById(meta.itemId);
            if (item) {
                const db = mongoose.connection.db;
                if (!db) throw new Error("Database connection missing db object");

                let deliverySuccess = false;
                let deliveryDetail = "";

                if (item.deliveryType === "currency") {
                    const field = item.deliveryData?.type === "gems" ? "gems" : "shards";
                    const amount = item.deliveryData?.amount || 0;
                    deliveryDetail = `${amount} ${field}`;

                    const updateResult = await db.collection("players").updateOne(
                        { uuid: meta.minecraftUuid },
                        { $inc: { [field]: amount } }
                    );
                    
                    deliverySuccess = updateResult.matchedCount > 0;
                } else if (item.deliveryType === "command" && item.deliveryData?.command) {
                    const command = item.deliveryData.command.replace("{player}", meta.minecraftUsername || "");
                    deliveryDetail = `Command: ${command}`;
                    
                    await db.collection("pending_commands").insertOne({
                        uuid: meta.minecraftUuid,
                        command: command,
                        executed: false,
                        createdAt: new Date(),
                        stripeSessionId: session.id
                    });
                    deliverySuccess = true;
                } else {
                    deliverySuccess = true;
                    deliveryDetail = "Manual/Default Delivery";
                }

                if (deliverySuccess) {
                    await Purchase.findByIdAndUpdate(purchase._id, { status: "delivered", deliveredAt: new Date() });
                    
                    // --- LOGGING: DELIVERY SUCCESS ---
                    try {
                        const { recordWebLog } = await import("@/lib/logger");
                        await recordWebLog({
                            type: "STORE_DELIVERY",
                            executor: { discordId: "system", name: "SYSTEM (Delivery)" },
                            summary: `Successfully delivered ${item.name} to ${meta.minecraftUsername || meta.minecraftUuid}`,
                            metadata: { 
                                type: item.deliveryType, 
                                detail: deliveryDetail,
                                minecraftUuid: meta.minecraftUuid 
                            }
                        });
                    } catch (le) { console.error("Log error:", le); }
                } else {
                    await Purchase.findByIdAndUpdate(purchase._id, { status: "failed" });
                }
            }
        } catch (dbErr: any) {
            console.error("❌ Webhook Database Error:", dbErr.message);
            return NextResponse.json({ error: "Internal processing error" }, { status: 500 });
        }
    }
  return NextResponse.json({ received: true });
}
