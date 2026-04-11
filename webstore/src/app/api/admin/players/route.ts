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
  
  const updateResult = await db.collection("players").updateOne(
    { uuid },
    { $set: { shards: Number(shards), gems: Number(gems), level: Number(level), bounty: Number(bounty) } }
  );

  return NextResponse.json({ success: true, matched: updateResult.matchedCount });
}
