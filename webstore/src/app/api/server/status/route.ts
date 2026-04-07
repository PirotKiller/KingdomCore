import { NextResponse } from "next/server";

export const revalidate = 60; // Cache for 60 seconds

export async function GET() {
  try {
    const res = await fetch("https://api.mcsrvstat.us/2/pic.thekingdom.net", {
      next: { revalidate: 60 }
    });
    const data = await res.json();

    return NextResponse.json({
      online: data.online,
      players: {
        online: data.players?.online || 0,
        max: data.players?.max || 100
      }
    });
  } catch (error) {
    return NextResponse.json({
      online: false,
      players: { online: 0, max: 100 }
    });
  }
}
