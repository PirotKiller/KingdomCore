export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { WebUser } from "@/models/WebUser";
import { ShopItem } from "@/models/ShopItem";
import { PendingCommand } from "@/models/PendingCommand";

async function isAdmin() {
  const session = await auth();
  if (!session?.user) return false;
  await dbConnect();
  const user = await WebUser.findOne({ discordId: (session as any).discordId });
  return user?.isAdmin === true;
}

// Valid shop types that match the Java ShopType enum
const VALID_SHOP_TYPES = [
  "wood", "stone", "fisherman", "fletcher", "redstone", "farming",
  "enchant", "potion", "nether", "end", "armor", "converter"
];

/**
 * GET /api/admin/shops
 * Fetch all shop items, optionally filtered by ?shopType=xxx
 * Returns items grouped by shopType.
 */
export async function GET(req: NextRequest) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  try {
    await dbConnect();
    const shopType = req.nextUrl.searchParams.get("shopType");

    const filter: any = {};
    if (shopType && VALID_SHOP_TYPES.includes(shopType)) {
      filter.shopType = shopType;
    }

    const items = await ShopItem.find(filter).sort({ shopType: 1, order: 1 }).lean();

    // Group by shopType
    const grouped: Record<string, any[]> = {};
    for (const type of VALID_SHOP_TYPES) {
      grouped[type] = [];
    }
    for (const item of items) {
      const type = item.shopType.toLowerCase();
      if (!grouped[type]) grouped[type] = [];
      grouped[type].push(item);
    }

    return NextResponse.json(grouped);
  } catch (error) {
    console.error("Failed to fetch shop items:", error);
    return NextResponse.json({ error: "Failed to fetch shop items" }, { status: 500 });
  }
}

/**
 * POST /api/admin/shops
 * Create a new shop item.
 */
export async function POST(req: NextRequest) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  try {
    await dbConnect();
    const body = await req.json();

    if (!body.shopType || !VALID_SHOP_TYPES.includes(body.shopType)) {
      return NextResponse.json({ error: "Invalid shopType" }, { status: 400 });
    }
    if (!body.name || !body.material) {
      return NextResponse.json({ error: "name and material are required" }, { status: 400 });
    }

    // Auto-generate itemKey if not provided
    if (!body.itemKey) {
      body.itemKey = body.name.toLowerCase().replace(/§./g, "").replace(/[^a-z0-9]/g, "_").replace(/_+/g, "_");
    }

    // Auto-assign order to the end
    if (body.order === undefined) {
      const maxOrder = await ShopItem.findOne({ shopType: body.shopType }).sort({ order: -1 }).select("order").lean();
      body.order = maxOrder ? (maxOrder as any).order + 1 : 0;
    }

    const item = await ShopItem.create(body);

    // --- LOGGING ---
    try {
      const { recordWebLog, LogType } = await import("@/lib/logger");
      const session = await auth();
      await recordWebLog({
        type: LogType.ADMIN_ACTION,
        executor: { discordId: (session as any)?.discordId, name: session?.user?.name || "Admin" },
        summary: `Added item to ${item.shopType} shop: ${item.name}`,
        metadata: { action: "SHOP_ITEM_CREATE", itemId: item._id, shop: item.shopType }
      });
    } catch (e) {}

    return NextResponse.json(item, { status: 201 });
  } catch (error) {
    console.error("Failed to create shop item:", error);
    return NextResponse.json({ error: "Failed to create shop item" }, { status: 500 });
  }
}

/**
 * PUT /api/admin/shops
 * Update an existing shop item by _id.
 */
export async function PUT(req: NextRequest) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  try {
    await dbConnect();
    const body = await req.json();
    const { _id, ...updateData } = body;

    if (!_id) return NextResponse.json({ error: "Missing _id" }, { status: 400 });

    const updated = await ShopItem.findByIdAndUpdate(_id, updateData, { new: true }).lean();
    if (!updated) return NextResponse.json({ error: "Item not found" }, { status: 404 });

    // --- LOGGING ---
    try {
      const { recordWebLog, LogType } = await import("@/lib/logger");
      const session = await auth();
      await recordWebLog({
        type: LogType.ADMIN_ACTION,
        executor: { discordId: (session as any)?.discordId, name: session?.user?.name || "Admin" },
        summary: `Updated item in ${updated.shopType} shop: ${updated.name}`,
        metadata: { action: "SHOP_ITEM_UPDATE", itemId: _id, shop: updated.shopType }
      });
    } catch (e) {}

    return NextResponse.json(updated);
  } catch (error) {
    console.error("Failed to update shop item:", error);
    return NextResponse.json({ error: "Failed to update shop item" }, { status: 500 });
  }
}

/**
 * DELETE /api/admin/shops
 * Delete a shop item by _id.
 */
export async function DELETE(req: NextRequest) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  try {
    await dbConnect();
    const { _id } = await req.json();
    if (!_id) return NextResponse.json({ error: "Missing _id" }, { status: 400 });

    const item = await ShopItem.findById(_id);
    if (!item) return NextResponse.json({ error: "Item not found" }, { status: 404 });
    const name = item.name;
    const shop = item.shopType;
    await ShopItem.findByIdAndDelete(_id);

    // --- LOGGING ---
    try {
      const { recordWebLog, LogType } = await import("@/lib/logger");
      const session = await auth();
      await recordWebLog({
        type: LogType.ADMIN_ACTION,
        executor: { discordId: (session as any)?.discordId, name: session?.user?.name || "Admin" },
        summary: `Removed item from ${shop} shop: ${name}`,
        metadata: { action: "SHOP_ITEM_DELETE", itemId: _id, shop }
      });
    } catch (e) {}

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("Failed to delete shop item:", error);
    return NextResponse.json({ error: "Failed to delete shop item" }, { status: 500 });
  }
}

/**
 * PATCH /api/admin/shops
 * Trigger a sync (reload) on the Minecraft server.
 */
export async function PATCH(req: NextRequest) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  try {
    await dbConnect();
    
    // Insert a reload command for the CONSOLE
    await PendingCommand.create({
      command: "shop reload",
      uuid: "CONSOLE",
      executed: false
    });

    // --- LOGGING ---
    try {
      const { recordWebLog, LogType } = await import("@/lib/logger");
      const session = await auth();
      await recordWebLog({
        type: LogType.ADMIN_ACTION,
        executor: { discordId: (session as any)?.discordId, name: session?.user?.name || "Admin" },
        summary: `Triggered Shop Sync (reload) to Minecraft server`,
        metadata: { action: "SHOP_SYNC" }
      });
    } catch (e) {}

    return NextResponse.json({ success: true, message: "Sync command sent to server" });
  } catch (error) {
    console.error("Failed to trigger sync:", error);
    return NextResponse.json({ error: "Failed to trigger sync" }, { status: 500 });
  }
}
