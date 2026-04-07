export const dynamic = "force-dynamic";

import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { WebUser } from "@/models/WebUser";
import { Purchase } from "@/models/Purchase";
import { StoreItem } from "@/models/StoreItem";
import mongoose from "mongoose";

async function isAdmin() {
  const session = await auth();
  if (!session?.user) return false;
  await dbConnect();
  const user = await WebUser.findOne({ discordId: (session as any).discordId });
  return user?.isAdmin === true;
}

export async function GET() {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  await dbConnect();

  const totalUsers = await WebUser.countDocuments();
  const verifiedUsers = await WebUser.countDocuments({ minecraftUuid: { $ne: null } });
  const totalPurchases = await Purchase.countDocuments();
  const totalRevenue = await Purchase.aggregate([
    { $match: { status: { $in: ["completed", "delivered"] } } },
    { $group: { _id: null, total: { $sum: "$price" } } },
  ]);
  const activeItems = await StoreItem.countDocuments({ active: true });

  // In-game stats from players collection
  const db = mongoose.connection.db;
  if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });
  
  const totalInGamePlayers = await db.collection("players").countDocuments();
  const onlinePlayers = await db.collection("players").countDocuments({ online: true });
  const activeAuctions = await db.collection("auctions").countDocuments();
  
  const totalBountyResult = await db.collection("players").aggregate([
    { $group: { _id: null, total: { $sum: "$bounty" } } }
  ]).toArray();
  const totalBounty = totalBountyResult[0]?.total || 0;

  // Recent purchases with user details
  const recentPurchases = await Purchase.find()
    .sort({ createdAt: -1 })
    .limit(5)
    .populate("userId", "discordUsername minecraftUsername");

  return NextResponse.json({
    totalUsers,
    verifiedUsers,
    totalPurchases,
    totalRevenue: totalRevenue[0]?.total || 0,
    activeItems,
    totalInGamePlayers,
    onlinePlayers,
    activeAuctions,
    totalBounty,
    recentPurchases,
  });
}
