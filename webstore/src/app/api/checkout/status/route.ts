export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import dbConnect from "@/lib/mongodb";
import { Purchase } from "@/models/Purchase";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const sessionId = searchParams.get("session_id");

  if (!sessionId) {
    return NextResponse.json({ error: "Session ID required" }, { status: 400 });
  }

  try {
    await dbConnect();
    
    // Find the purchase by Stripe Session ID
    const purchase = await Purchase.findOne({ stripeSessionId: sessionId })
      .select("transactionId itemName status price currency createdAt -_id");

    if (!purchase) {
      return NextResponse.json({ error: "Order not found" }, { status: 404 });
    }

    return NextResponse.json(purchase);
  } catch (error: any) {
    console.error("Error fetching checkout status:", error.message);
    return NextResponse.json({ error: "Internal server error" }, { status: 500 });
  }
}
