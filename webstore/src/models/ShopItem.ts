import mongoose, { Schema, model, models } from "mongoose";

const ShopItemSchema = new Schema({
  shopType: { type: String, required: true, index: true },
  itemKey: { type: String, required: true },
  name: { type: String, required: true },
  material: { type: String, required: true },
  amount: { type: Number, default: 1 },
  lore: { type: [String], default: [] },
  priceShards: { type: Number, default: 0 },
  priceGems: { type: Number, default: 0 },
  enchant: { type: String, default: null },
  enchantLevel: { type: Number, default: 0 },
  damage: { type: Number, default: 0 },
  speed: { type: Number, default: 0 },
  class: { type: String, default: null },
  tier: { type: String, default: null },
  cmd: { type: Number, default: 0 },
  order: { type: Number, default: 0 },
  active: { type: Boolean, default: true },
}, { timestamps: true });

// Compound index for efficient lookups
ShopItemSchema.index({ shopType: 1, order: 1 });

export const ShopItem = models?.ShopItem || model("ShopItem", ShopItemSchema, "shops");
