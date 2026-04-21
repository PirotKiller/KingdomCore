export const dynamic = "force-dynamic";

import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { WebUser } from "@/models/WebUser";
import mongoose from "mongoose";

export async function GET(request: Request) {
  const session = await auth();
  if (!session?.user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  await dbConnect();
  const db = mongoose.connection.db;
  if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });
  
  const user = await WebUser.findOne({ discordId: (session as any).discordId });
  if (!user?.isAdmin) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { searchParams } = new URL(request.url);
  const page = parseInt(searchParams.get("page") || "1");
  const limit = parseInt(searchParams.get("limit") || "40");
  const skip = (page - 1) * limit;
  const search = searchParams.get("search") || "";
  const typeUrl = searchParams.get("type") || "";
  const sourceUrl = searchParams.get("source") || "ALL";

  let query: any = {};
  
  // Filter by source (GAME or WEB)
  if (sourceUrl && sourceUrl !== "ALL") {
    query.source = sourceUrl;
  }

  // Text search against playerName or details (supporting both old and new formats)
  if (search) {
    query.$or = [
      { playerName: { $regex: search, $options: "i" } },
      { "player.name": { $regex: search, $options: "i" } },
      { details: { $regex: search, $options: "i" } },
      { summary: { $regex: search, $options: "i" } }
    ];
  }
  
  // Filter by event type (supporting both old 'eventType' and new 'type' fields)
  if (typeUrl && typeUrl !== "ALL") {
    query.$or = [
      { eventType: typeUrl },
      { type: typeUrl }
    ];
  }

  const total = await db.collection("game_logs").countDocuments(query);
  const rawLogs = await db.collection("game_logs")
    .find(query)
    .sort({ timestamp: -1 })
    .skip(skip)
    .limit(limit)
    .toArray();

  // Normalize logs for the frontend
  const logs = rawLogs.map(log => ({
    ...log,
    playerName: log.playerName || log.player?.name || "System",
    details: log.details || log.summary || "",
    eventType: log.eventType || log.type || "INFO",
  }));

  return NextResponse.json({
    logs,
    total,
    page,
    totalPages: Math.ceil(total / limit)
  });
}
