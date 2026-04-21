export const dynamic = "force-dynamic";

import { NextResponse } from "next/server";
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

export async function GET(
  request: Request,
  { params }: { params: Promise<{ playerName: string }> }
) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { playerName } = await params;

  try {
    await dbConnect();
    const db = mongoose.connection.db;
    if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });

    // 1. Get Moderation logs
    const punishments = await db.collection("punishments")
      .find({ playerName: { $regex: new RegExp(`^${playerName}$`, "i") } })
      .sort({ issuedAt: -1 })
      .toArray();

    const moderationLogs = punishments.map(p => {
      let duration = "Permanent";
      if (p.expireAt && p.issuedAt) {
        const diff = new Date(p.expireAt).getTime() - new Date(p.issuedAt).getTime();
        const hrs = Math.round(diff / (1000 * 60 * 60));
        duration = hrs >= 24 ? `${Math.round(hrs / 24)}d` : `${hrs}h`;
      }

      return {
        _id: p._id,
        action: p.type?.toLowerCase() || "unknown",
        reason: p.reason,
        adminName: p.adminName || "System",
        duration: duration,
        createdAt: p.issuedAt || p.createdAt
      };
    });

    // 2. Get Game logs (Chat/Commands)
    const gameLogs = await db.collection("game_logs")
      .find({ playerName: { $regex: new RegExp(`^${playerName}$`, "i") } })
      .sort({ timestamp: -1 })
      .limit(100)
      .toArray();

    // 3. Get Purchases
    // We need to find the UUID first from PlayerData or WebUser
    const user = await WebUser.findOne({ minecraftUsername: { $regex: new RegExp(`^${playerName}$`, "i") } });
    const playerRecord = await db.collection("players").findOne({ lastKnownName: { $regex: new RegExp(`^${playerName}$`, "i") } });
    
    const uuid = user?.minecraftUuid || playerRecord?.uuid;
    
    let purchases: any[] = [];
    if (uuid) {
      purchases = await db.collection("purchases")
        .find({ minecraftUuid: uuid })
        .sort({ createdAt: -1 })
        .toArray();
    }

    return NextResponse.json({
      playerName,
      moderationLogs,
      gameLogs,
      purchases,
      linkedUser: user ? {
        discordId: user.discordId,
        discordUsername: user.discordUsername,
        avatar: user.discordAvatar
      } : null
    });
  } catch (error: any) {
    console.error("Error fetching player unified history:", error.message);
    return NextResponse.json({ error: "Failed to fetch player history" }, { status: 500 });
  }
}
