export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { getStripe } from "@/lib/stripe";
import dbConnect from "@/lib/mongodb";
import { StoreItem } from "@/models/StoreItem";
import { WebUser } from "@/models/WebUser";

export async function POST(req: NextRequest) {
  const session = await auth();
  if (!session?.user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const discordId = (session as any).discordId;
  await dbConnect();

  const user = await WebUser.findOne({ discordId });
  if (!user || !user.minecraftUuid) {
    return NextResponse.json({ error: "Link your Minecraft account first" }, { status: 400 });
  }

  const body = await req.json();
  const { itemId } = body;

  const item = await StoreItem.findById(itemId);
  if (!item || !item.active) {
    return NextResponse.json({ error: "Item not found" }, { status: 404 });
  }

  const checkoutSession = await getStripe().checkout.sessions.create({
    payment_method_types: ["card"],
    line_items: [
      {
        price_data: {
          currency: item.currency.toLowerCase(),
          product_data: {
            name: item.name,
            description: item.description,
          },
          unit_amount: item.price,
        },
        quantity: 1,
      },
    ],
    mode: "payment",
    success_url: `${process.env.AUTH_URL || "http://localhost:3000"}/store/success?session_id={CHECKOUT_SESSION_ID}`,
    cancel_url: `${process.env.AUTH_URL || "http://localhost:3000"}/store`,
    metadata: {
      userId: user._id.toString(),
      discordId: user.discordId,
      minecraftUuid: user.minecraftUuid,
      itemId: item._id.toString(),
      itemName: item.name,
    },
  });

  return NextResponse.json({ url: checkoutSession.url });
}
