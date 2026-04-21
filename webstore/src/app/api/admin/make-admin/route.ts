export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import dbConnect from "@/lib/mongodb";
import { WebUser } from "@/models/WebUser";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const secret = searchParams.get("secret");
  const username = searchParams.get("username");

  if (secret !== "kingdom-setup") {
    return NextResponse.json({ error: "Invalid secret" }, { status: 403 });
  }

  try {
    await dbConnect();

    // List all users first
    const allUsers = await WebUser.find({});
    const usernames = allUsers.map((u: any) => ({
      name: u.discordUsername,
      isAdmin: u.isAdmin,
      id: u.discordId,
    }));

    if (!username) {
      return NextResponse.json({ users: usernames });
    }

    // Try exact match first, then case-insensitive
    let user = await WebUser.findOne({ discordUsername: username });
    if (!user) {
      user = await WebUser.findOne({
        discordUsername: { $regex: new RegExp("^" + username + "$", "i") },
      });
    }

    if (!user) {
      return NextResponse.json({
        error: "User not found",
        availableUsers: usernames,
      });
    }

    user.isAdmin = true;
    await user.save();

    // --- LOGGING ---
    try {
      const { recordWebLog, LogType } = await import("@/lib/logger");
      await recordWebLog({
        type: LogType.ADMIN_ACTION,
        executor: {
          name: "SYSTEM (Setup Secret)",
        },
        target: {
          discordId: user.discordId,
          name: user.discordUsername,
        },
        summary: `Promoted ${user.discordUsername} to Admin via setup secret`,
        metadata: { action: "MAKE_ADMIN", discordId: user.discordId }
      });
    } catch (logError) {
      console.error("Failed to record unified log:", logError);
    }

    return NextResponse.json({
      success: true,
      message: user.discordUsername + " is now an admin!",
    });
  } catch (err: any) {
    return NextResponse.json({ error: err.message }, { status: 500 });
  }
}
