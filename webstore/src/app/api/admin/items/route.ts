export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { StoreItem } from "@/models/StoreItem";
import { WebUser } from "@/models/WebUser";

async function isAdmin() {
  const session = await auth();
  if (!session?.user) return false;
  await dbConnect();
  const user = await WebUser.findOne({ discordId: (session as any).discordId });
  return user?.isAdmin === true;
}

// GET — list store items with search and pagination
export async function GET(req: NextRequest) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { searchParams } = new URL(req.url);
  const search = searchParams.get("search") || "";
  const page = parseInt(searchParams.get("page") || "1");
  const limit = parseInt(searchParams.get("limit") || "20");
  const skip = (page - 1) * limit;

  await dbConnect();

  const filter: any = {};
  if (search) {
    filter.name = { $regex: search, $options: "i" };
  }

  const total = await StoreItem.countDocuments(filter);
  const items = await StoreItem.find(filter)
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit);

  return NextResponse.json({
    items,
    total,
    page,
    totalPages: Math.ceil(total / limit),
  });
}

// POST — create new store item
export async function POST(req: NextRequest) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  await dbConnect();
  const body = await req.json();
  const item = await StoreItem.create(body);
  return NextResponse.json(item, { status: 201 });
}

// PUT — update store item
export async function PUT(req: NextRequest) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  await dbConnect();
  const body = await req.json();
  const { _id, ...updates } = body;
  const item = await StoreItem.findByIdAndUpdate(_id, updates, { new: true });
  return NextResponse.json(item);
}

// PATCH — quick-toggle active or featured
export async function PATCH(req: NextRequest) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  await dbConnect();
  const { id, field } = await req.json();

  if (!["active", "featured"].includes(field)) {
    return NextResponse.json({ error: "Invalid field" }, { status: 400 });
  }

  const item = await StoreItem.findById(id);
  if (!item) {
    return NextResponse.json({ error: "Item not found" }, { status: 404 });
  }

  item[field] = !item[field];
  await item.save();

  return NextResponse.json({ success: true, [field]: item[field] });
}

// DELETE — delete store item
export async function DELETE(req: NextRequest) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  await dbConnect();
  const { id } = await req.json();
  await StoreItem.findByIdAndDelete(id);
  return NextResponse.json({ success: true });
}
