export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import dbConnect from "@/lib/mongodb";
import { StoreItem } from "@/models/StoreItem";

export async function GET(req: NextRequest) {
  await dbConnect();
  const { searchParams } = new URL(req.url);
  const category = searchParams.get("category");
  const featured = searchParams.get("featured");

  const filter: any = { active: true };
  if (category) filter.category = category;
  if (featured === "true") filter.featured = true;

  const items = await StoreItem.find(filter).sort({ featured: -1, createdAt: -1 });
  return NextResponse.json(items);
}
