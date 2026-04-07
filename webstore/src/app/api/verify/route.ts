export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { Verification } from "@/models/Verification";
import { WebUser } from "@/models/WebUser";

function generateCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

// POST /api/verify — generate a verification code
export async function POST(req: NextRequest) {
  const session = await auth();
  if (!session?.user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const discordId = (session as any).discordId;
  if (!discordId) {
    return NextResponse.json({ error: "Discord ID not found" }, { status: 400 });
  }

  await dbConnect();

  // Delete any existing codes for this user
  await Verification.deleteMany({ discordId });

  const code = generateCode();
  await Verification.create({ code, discordId });

  return NextResponse.json({ code });
}

// GET /api/verify — check if the user is verified
export async function GET(req: NextRequest) {
  const session = await auth();
  if (!session?.user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const discordId = (session as any).discordId;
  await dbConnect();

  const user = await WebUser.findOne({ discordId });
  if (!user) {
    return NextResponse.json({ verified: false });
  }

  return NextResponse.json({
    verified: !!user.minecraftUuid,
    minecraftUsername: user.minecraftUsername || null,
    minecraftUuid: user.minecraftUuid || null,
  });
}
