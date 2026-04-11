export const dynamic = "force-dynamic";

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

  let event;
  try {
    event = getStripe().webhooks.constructEvent(body, sig, process.env.STRIPE_WEBHOOK_SECRET!);
  } catch (err: any) {
    console.error("Webhook Error: Signature verification failed:", err.message);
    return NextResponse.json({ error: "Invalid signature" }, { status: 400 });
  }

  if (event.type === "checkout.session.completed") {
    const session = event.data.object;
    const meta = session.metadata;

    if (!meta) {
      console.warn("Webhook Warning: No metadata in session:", session.id);
      return NextResponse.json({ received: true });
    }

    try {
      console.log("Processing Webhook: checkout.session.completed for", session.id);
      await dbConnect();

      // Verify we have a primary connection
      if (mongoose.connection.readyState !== 1) {
        throw new Error("Database connection not ready (readyState: " + mongoose.connection.readyState + ")");
      }

      // Record the purchase
      console.log("Recording purchase for userId:", meta.userId);
      await Purchase.create({
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
      });

      // For currency purchases, update the player document directly
      const item = await StoreItem.findById(meta.itemId);
      if (item) {
        const db = mongoose.connection.db;
        if (!db) throw new Error("Database connection is missing db object");

        if (item.deliveryType === "currency") {
          const field = item.deliveryData?.type === "gems" ? "gems" : "shards";
          const amount = item.deliveryData?.amount || 0;

          console.log("Delivering currency:", amount, field, "to", meta.minecraftUuid);
          const updateResult = await db.collection("players").updateOne(
            { uuid: meta.minecraftUuid },
            { $inc: { [field]: amount } }
          );
            
          if (updateResult.matchedCount === 0) {
            console.error("Delivery Error: Player not found in database:", meta.minecraftUuid);
          } else {
            console.log("Delivery Success: Updated player document.");
            await Purchase.findOneAndUpdate({ stripeSessionId: session.id }, { status: "delivered", deliveredAt: new Date() });
          }
        } else if (item.deliveryType === "command" && item.deliveryData?.command) {
          console.log("Queueing command for delivery:", item.deliveryData.command, "to", meta.minecraftUuid);
          await db.collection("pending_commands").insertOne({
            uuid: meta.minecraftUuid,
            command: item.deliveryData.command,
            executed: false,
            createdAt: new Date(),
            stripeSessionId: session.id
          });
          
          console.log("Delivery Success: Enqueued command.");
          await Purchase.findOneAndUpdate({ stripeSessionId: session.id }, { status: "processing", deliveredAt: new Date() });
        }
      }
      
      console.log("Webhook Success: Processed session", session.id);
    } catch (dbErr: any) {
      console.error("Webhook Database Error:", dbErr.message);
      // We still return 200 so Stripe doesn't keep retrying if it's a persistent DB issue
      return NextResponse.json({ error: "Database error during processing" }, { status: 500 });
    }
  }

  return NextResponse.json({ received: true });
}
