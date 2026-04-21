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

export async function GET(request: Request) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  
  const { searchParams } = new URL(request.url);
  const page = parseInt(searchParams.get("page") || "1");
  const limit = parseInt(searchParams.get("limit") || "20");
  const skip = (page - 1) * limit;

  await dbConnect();
  
  const db = mongoose.connection.db;
  if (!db) {
    return NextResponse.json({ error: "Database not connected" }, { status: 500 });
  }
  
  const total = await db.collection("players").countDocuments();
  const players = await db.collection("players")
    .find()
    .sort({ lastKnownName: 1 })
    .skip(skip)
    .limit(limit)
    .toArray();

  return NextResponse.json({
    players,
    total,
    page,
    totalPages: Math.ceil(total / limit)
  });
}

export async function PUT(req: NextRequest) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { uuid, shards, gems, level, bounty } = await req.json();
  if (!uuid) return NextResponse.json({ error: "UUID required" }, { status: 400 });

  await dbConnect();
  
  const db = mongoose.connection.db;
  if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });
  
  // Fetch current data for logging comparison (optional, but good for summary)
  const player = await db.collection("players").findOne({ uuid });
  const playerName = player?.lastKnownName || "Unknown";

  const updateResult = await db.collection("players").updateOne(
    { uuid },
    { $set: { shards: Number(shards), gems: Number(gems), level: Number(level), bounty: Number(bounty) } }
  );

  if (updateResult.matchedCount > 0) {
    const { recordWebLog, LogType } = await import("@/lib/logger");
    const session = await auth();
    
    await recordWebLog({
      type: LogType.ADMIN_ACTION,
      executor: {
        discordId: (session as any)?.discordId,
        name: session?.user?.name || "Unknown Admin",
      },
      target: {
        uuid,
        name: playerName,
      },
      summary: `Updated player stats (Shards: ${shards}, Gems: ${gems}, Lvl: ${level}, Bounty: ${bounty})`,
      currency: {
        shards: Number(shards),
        gems: Number(gems),
      },
      metadata: { action: "PLAYER_UPDATE", originalShards: player?.shards, originalGems: player?.gems }
    });
  }

  return NextResponse.json({ success: true, matched: updateResult.matchedCount });
}
