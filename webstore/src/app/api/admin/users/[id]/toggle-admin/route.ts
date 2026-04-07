import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { WebUser } from "@/models/WebUser";

async function isAdminData() {
  const session = await auth();
  if (!session?.user) return null;
  await dbConnect();
  return await WebUser.findOne({ discordId: (session as any).discordId });
}

export async function POST(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const admin = await isAdminData();
  if (!admin?.isAdmin) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { id } = await params;

  // Prevent self-demotion
  if (admin._id.toString() === id) {
    return NextResponse.json({ error: "You cannot demote yourself" }, { status: 400 });
  }

  await dbConnect();
  
  const user = await WebUser.findById(id);
  if (!user) {
    return NextResponse.json({ error: "User not found" }, { status: 404 });
  }

  // Toggle admin status
  user.isAdmin = !user.isAdmin;
  await user.save();

  return NextResponse.json({ success: true, isAdmin: user.isAdmin });
}
