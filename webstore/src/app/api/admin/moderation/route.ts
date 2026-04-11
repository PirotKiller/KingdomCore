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

// POST: Execute a moderation action
export async function POST(req: NextRequest) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { action, playerName, reason, duration, customCommand } = await req.json();

  if (!action) {
    return NextResponse.json({ error: "Action is required" }, { status: 400 });
  }

  await dbConnect();
  const db = mongoose.connection.db;
  if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });

  // Get the admin's identity for the log
  const session = await auth();
  const adminName = (session as any)?.user?.name || "Unknown Admin";

  let command = "";
  let label = "";

  switch (action) {
    case "kick":
      if (!playerName) return NextResponse.json({ error: "Player name required" }, { status: 400 });
      command = reason ? `kick ${playerName} ${reason}` : `kick ${playerName}`;
      label = `Kicked ${playerName}`;
      break;
    case "ban":
      if (!playerName) return NextResponse.json({ error: "Player name required" }, { status: 400 });
      command = reason ? `ban ${playerName} ${reason}` : `ban ${playerName}`;
      label = `Banned ${playerName}`;
      break;
    case "tempban":
      if (!playerName || !duration) return NextResponse.json({ error: "Player name and duration required" }, { status: 400 });
      command = reason 
        ? `tempban ${playerName} ${duration} ${reason}` 
        : `tempban ${playerName} ${duration}`;
      label = `Temp-banned ${playerName} for ${duration}`;
      break;
    case "unban":
      if (!playerName) return NextResponse.json({ error: "Player name required" }, { status: 400 });
      command = `pardon ${playerName}`;
      label = `Unbanned ${playerName}`;
      break;
    case "mute":
      if (!playerName) return NextResponse.json({ error: "Player name required" }, { status: 400 });
      command = duration 
        ? `mute ${playerName} ${duration}` 
        : `mute ${playerName}`;
      label = duration ? `Muted ${playerName} for ${duration}` : `Muted ${playerName}`;
      break;
    case "unmute":
      if (!playerName) return NextResponse.json({ error: "Player name required" }, { status: 400 });
      command = `unmute ${playerName}`;
      label = `Unmuted ${playerName}`;
      break;
    case "warn":
      if (!playerName) return NextResponse.json({ error: "Player name required" }, { status: 400 });
      command = reason 
        ? `warn ${playerName} ${reason}` 
        : `warn ${playerName}`;
      label = `Warned ${playerName}`;
      break;
    case "custom":
      if (!customCommand) return NextResponse.json({ error: "Command is required" }, { status: 400 });
      command = customCommand;
      label = `Custom: ${customCommand.substring(0, 50)}`;
      break;
    default:
      return NextResponse.json({ error: "Unknown action" }, { status: 400 });
  }

  if (action === "custom") {
    // Queue the custom command for the server console
    await db.collection("web_commands").insertOne({
      command,
      uuid: "CONSOLE",
      executed: false,
      createdAt: new Date(),
      source: "moderation"
    });
  } else {
    // Queue native moderation action for the plugin to process internally
    await db.collection("moderation_queue").insertOne({
      action,
      playerName: playerName || null,
      reason: reason || null,
      duration: duration || null,
      adminName,
      executed: false,
      createdAt: new Date()
    });
  }

  // Log the moderation action
  await db.collection("moderation_logs").insertOne({
    action,
    playerName: playerName || null,
    reason: reason || null,
    duration: duration || null,
    command,
    label,
    adminName,
    createdAt: new Date()
  });

  return NextResponse.json({ success: true, label });
}

// GET: Retrieve moderation logs
export async function GET(request: Request) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { searchParams } = new URL(request.url);
  const page = parseInt(searchParams.get("page") || "1");
  const limit = parseInt(searchParams.get("limit") || "20");
  const skip = (page - 1) * limit;
  const playerName = searchParams.get("playerName");

  await dbConnect();
  const db = mongoose.connection.db;
  if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });

  let query: any = {};
  if (playerName) {
    query.playerName = { $regex: playerName, $options: "i" };
  }

  const total = await db.collection("moderation_logs").countDocuments(query);
  const logs = await db.collection("moderation_logs")
    .find(query)
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit)
    .toArray();

  return NextResponse.json({
    logs,
    total,
    page,
    totalPages: Math.ceil(total / limit)
  });
}
