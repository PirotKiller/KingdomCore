export const dynamic = "force-dynamic";

import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { WebUser } from "@/models/WebUser";
import { Purchase } from "@/models/Purchase";

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
  const purchases = await Purchase.find().sort({ createdAt: -1 }).limit(100);
  return NextResponse.json(purchases);
}
