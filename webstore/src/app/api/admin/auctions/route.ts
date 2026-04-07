export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { WebUser } from "@/models/WebUser";
import mongoose from "mongoose";

async function isAdmin() {
  const session = await auth();
  if (!session?.user) return false;
  await dbConnect();
  const user = await WebUser.findOne({ discordId: (session as any).discordId });
  return user?.isAdmin === true;
}

export async function GET() {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  
  await dbConnect();
  const db = mongoose.connection.db;
  if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });
  
  const auctions = await db.collection("auctions").find().sort({ expireTime: 1 }).toArray();
  return NextResponse.json(auctions);
}

export async function PUT(req: NextRequest) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  const { listingId, priceShards, priceGems } = await req.json();
  if (!listingId) return NextResponse.json({ error: "listingId required" }, { status: 400 });

  await dbConnect();
  const db = mongoose.connection.db;
  if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });
  
  const updateResult = await db.collection("auctions").updateOne(
    { listingId },
    { $set: { priceShards: Number(priceShards), priceGems: Number(priceGems) } }
  );

  return NextResponse.json({ success: true, matched: updateResult.matchedCount });
}

export async function DELETE(req: NextRequest) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  const { listingId } = await req.json();
  if (!listingId) return NextResponse.json({ error: "listingId required" }, { status: 400 });

  await dbConnect();
  const db = mongoose.connection.db;
  if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });
  
  await db.collection("auctions").deleteOne({ listingId });

  return NextResponse.json({ success: true });
}
