export const dynamic = "force-dynamic";

import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { Purchase } from "@/models/Purchase";
import { WebUser } from "@/models/WebUser";

export async function GET() {
  const session = await auth();
  if (!session?.user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  try {
    await dbConnect();
    const user = await WebUser.findOne({ discordId: (session as any).discordId });
    if (!user) {
      return NextResponse.json([]);
    }

    const purchases = await Purchase.find({ discordId: user.discordId }).sort({ createdAt: -1 });
    return NextResponse.json(purchases);
  } catch (error: any) {
    console.error("Error fetching purchases:", error.message);
    return NextResponse.json({ error: "Failed to fetch purchases" }, { status: 500 });
  }
}
