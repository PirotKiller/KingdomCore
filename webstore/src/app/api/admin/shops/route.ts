export const dynamic = "force-dynamic";

import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import dbConnect from "@/lib/mongodb";
import { WebUser } from "@/models/WebUser";
import fs from "fs/promises";
import path from "path";
import YAML from "yaml";

const SHOPS_DIR = path.join(process.cwd(), "..", "src", "main", "resources", "shops");

async function isAdmin() {
  const session = await auth();
  if (!session?.user) return false;
  await dbConnect();
  const user = await WebUser.findOne({ discordId: (session as any).discordId });
  return user?.isAdmin === true;
}

export async function GET() {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  try {
    const files = await fs.readdir(SHOPS_DIR);
    const shops: Record<string, any> = {};

    for (const file of files) {
      if (file.endsWith(".yml")) {
        const content = await fs.readFile(path.join(SHOPS_DIR, file), "utf-8");
        shops[file.replace(".yml", "")] = YAML.parse(content);
      }
    }

    return NextResponse.json(shops);
  } catch (error) {
    return NextResponse.json({ error: "Failed to read shop configs" }, { status: 500 });
  }
}

export async function PUT(req: NextRequest) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  try {
    const { shopId, content } = await req.json();
    if (!shopId || !content) return NextResponse.json({ error: "Invalid payload" }, { status: 400 });

    const filePath = path.join(SHOPS_DIR, `${shopId}.yml`);
    const yamlStr = YAML.stringify(content);

    await fs.writeFile(filePath, yamlStr, "utf-8");

    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json({ error: "Failed to save shop config" }, { status: 500 });
  }
}
