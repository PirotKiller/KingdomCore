export const dynamic = "force-dynamic";

import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { Purchase } from "@/models/Purchase";
import { WebUser } from "@/models/WebUser";
import mongoose from "mongoose";

export async function GET(request: Request) {
  const session = await auth();
  if (!session?.user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { searchParams } = new URL(request.url);
  const page = parseInt(searchParams.get("page") || "1");
  const limit = parseInt(searchParams.get("limit") || "10");
  const skip = (page - 1) * limit;

  try {
    await dbConnect();
    const db = mongoose.connection.db;
    if (!db) return NextResponse.json({ error: "Database not connected" }, { status: 500 });
    
    // Find the current user to get their Minecraft name
    const user = await WebUser.findOne({ discordId: (session as any).discordId });
    if (!user || !user.minecraftUsername) {
      return NextResponse.json({ logs: [], total: 0, totalPages: 0, page });
    }

    const query = { playerName: user.minecraftUsername };
    
    const total = await db.collection("punishments").countDocuments(query);
    const punishments = await db.collection("punishments")
      .find(query)
      .sort({ issuedAt: -1 })
      .skip(skip)
      .limit(limit)
      .toArray();

    const logs = punishments.map(p => {
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

    return NextResponse.json({
      logs,
      total,
      totalPages: Math.ceil(total / limit),
      page
    });
  } catch (error: any) {
    console.error("Error fetching history:", error.message);
    return NextResponse.json({ error: "Failed to fetch history" }, { status: 500 });
  }
}
