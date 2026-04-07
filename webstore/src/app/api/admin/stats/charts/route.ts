import { NextResponse } from "next/server";
import { Purchase } from "@/models/Purchase";
import { WebUser } from "@/models/WebUser";
import dbConnect from "@/lib/mongodb";
import { auth } from "@/lib/auth";

async function isAdmin() {
  const session = await auth();
  if (!session?.user) return false;
  await dbConnect();
  // Using discordId from session if available
  const user = await WebUser.findOne({ discordId: (session as any).discordId });
  return user?.isAdmin === true;
}

export async function GET() {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  await dbConnect();

  try {
    const fourteenDaysAgo = new Date();
    fourteenDaysAgo.setDate(fourteenDaysAgo.getDate() - 14);

    // Aggregate Purchases (Revenue & Sales)
    const purchaseStats = await Purchase.aggregate([
      {
        $match: {
          createdAt: { $gte: fourteenDaysAgo },
          status: { $in: ["completed", "delivered"] }
        }
      },
      {
        $group: {
          _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt" } },
          revenue: { $sum: "$price" },
          sales: { $sum: 1 }
        }
      }
    ]);

    // Aggregate Players (New Registrations)
    const playerStats = await WebUser.aggregate([
      {
        $match: {
          createdAt: { $gte: fourteenDaysAgo }
        }
      },
      {
        $group: {
          _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt" } },
          growth: { $sum: 1 }
        }
      }
    ]);

    // Fill in gaps for all 14 days
    const chartData = [];
    for (let i = 14; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dateStr = d.toISOString().split("T")[0];
      
      const pFound = purchaseStats.find(s => s._id === dateStr);
      const gFound = playerStats.find(s => s._id === dateStr);

      chartData.push({
        date: dateStr,
        revenue: pFound ? pFound.revenue / 100 : 0,
        sales: pFound ? pFound.sales : 0,
        growth: gFound ? gFound.growth : 0
      });
    }

    return NextResponse.json(chartData);
  } catch (error) {
    return NextResponse.json({ error: "Failed to fetch chart data" }, { status: 500 });
  }
}
