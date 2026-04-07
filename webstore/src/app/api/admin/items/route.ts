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

// GET — list all store items
export async function GET() {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  await dbConnect();
  const items = await StoreItem.find().sort({ createdAt: -1 });
  return NextResponse.json(items);
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
