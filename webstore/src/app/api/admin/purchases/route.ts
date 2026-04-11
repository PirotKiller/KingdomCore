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

export async function GET(request: Request) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { searchParams } = new URL(request.url);
  const search = searchParams.get("search") || "";
  const page = parseInt(searchParams.get("page") || "1");
  const limit = parseInt(searchParams.get("limit") || "20");
  const skip = (page - 1) * limit;

  await dbConnect();

  let query: any = {};
  if (search) {
    query = {
      $or: [
        { transactionId: { $regex: search, $options: "i" } },
        { itemName: { $regex: search, $options: "i" } },
        { discordId: { $regex: search, $options: "i" } },
        { minecraftUuid: { $regex: search, $options: "i" } }
      ]
    };
  }

  const total = await Purchase.countDocuments(query);
  const purchases = await Purchase.find(query)
    .populate("userId", "minecraftUsername")
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit);

  return NextResponse.json({
    purchases,
    total,
    page,
    totalPages: Math.ceil(total / limit)
  });
}
