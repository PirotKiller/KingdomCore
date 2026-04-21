import dbConnect from "./mongodb";
import mongoose from "mongoose";

export enum LogSource {
  GAME = "GAME",
  WEB = "WEB",
}

export enum LogType {
  SHOP_PURCHASE = "SHOP_PURCHASE",
  AH_LIST = "AH_LIST",
  AH_BUY = "AH_BUY",
  ECO_ADMIN = "ECO_ADMIN",
  ECO_WITHDRAW = "ECO_WITHDRAW",
  ECO_DEPOSIT = "ECO_DEPOSIT",
  BOUNTY_CLAIM = "BOUNTY_CLAIM",
  BOUNTY_SET = "BOUNTY_SET",
  CONVERTER_SELL = "CONVERTER_SELL",
  GEM_CONVERSION = "GEM_CONVERSION",
  ADMIN_ACTION = "ADMIN_ACTION",
  MODERATION = "MODERATION",
  STORE_CHECKOUT_START = "STORE_CHECKOUT_START",
  STORE_PURCHASE_COMPLETE = "STORE_PURCHASE_COMPLETE",
  STORE_DELIVERY = "STORE_DELIVERY",
  LEVEL_UP = "LEVEL_UP",
  CLASS_CHANGE = "CLASS_CHANGE",
}

interface LogData {
  source: LogSource;
  type: LogType | string;
  executor: {
    uuid?: string;
    discordId?: string;
    name: string;
  };
  target?: {
    uuid?: string;
    discordId?: string;
    name: string;
    nickname?: string;
  };
  summary: string;
  currency?: {
    shards: number;
    gems: number;
  };
  metadata?: Record<string, any>;
  timestamp?: Date;
}

/**
 * Records a log entry to the 'game_logs' collection.
 */
export async function recordWebLog(data: Omit<LogData, "source" | "timestamp">) {
  try {
    await dbConnect();
    const db = mongoose.connection.db;
    if (!db) {
      console.error("logger.ts: Database not connected");
      return;
    }

    const logEntry = {
      ...data,
      source: LogSource.WEB,
      timestamp: new Date(),
    };

    await db.collection("game_logs").insertOne(logEntry);
  } catch (error) {
    console.error("logger.ts: Failed to record log:", error);
  }
}
