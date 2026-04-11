export const dynamic = "force-dynamic";

import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { Purchase } from "@/models/Purchase";
import { WebUser } from "@/models/WebUser";

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
    const user = await WebUser.findOne({ discordId: (session as any).discordId });
    if (!user) {
      return NextResponse.json({ purchases: [], total: 0, totalPages: 0, page });
    }

    const query = { discordId: user.discordId };
    const total = await Purchase.countDocuments(query);
    const purchases = await Purchase.find(query)
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit);

    return NextResponse.json({
      purchases,
      total,
      totalPages: Math.ceil(total / limit),
      page
    });
  } catch (error: any) {
    console.error("Error fetching purchases:", error.message);
    return NextResponse.json({ error: "Failed to fetch purchases" }, { status: 500 });
  }
}
