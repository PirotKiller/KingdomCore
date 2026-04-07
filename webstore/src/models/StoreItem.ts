import mongoose, { Schema, model, models } from "mongoose";

const StoreItemSchema = new Schema({
  name: { type: String, required: true },
  description: { type: String, required: true },
  category: { type: String, enum: ["currency", "items", "ranks"], required: true },
  price: { type: Number, required: true }, // In cents (e.g., 1000 = $10.00)
  currency: { type: String, default: "USD" },
  imageUrl: { type: String },
  deliveryType: { type: String, enum: ["currency", "item", "command"], required: true },
  deliveryData: {
    type: { type: String }, // 'gems' or 'shards' for currency
    amount: { type: Number },
    material: { type: String }, // for items
    display: { type: String },
    command: { type: String }, // the command string to execute (use {player})
  },
  featured: { type: Boolean, default: false },
  active: { type: Boolean, default: true },
}, { timestamps: true });

export const StoreItem = models?.StoreItem || model("StoreItem", StoreItemSchema);
