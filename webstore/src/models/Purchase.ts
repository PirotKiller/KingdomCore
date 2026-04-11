import mongoose, { Schema, model, models } from "mongoose";

const PurchaseSchema = new Schema({
  userId: { type: Schema.Types.ObjectId, ref: "WebUser", required: true },
  discordId: { type: String, required: true },
  minecraftUuid: { type: String, required: true },
  itemId: { type: Schema.Types.ObjectId, ref: "StoreItem", required: true },
  itemName: { type: String, required: true },
  price: { type: Number, required: true },
  currency: { type: String, required: true },
  stripeSessionId: { type: String, required: true, unique: true },
  transactionId: { type: String, sparse: true, unique: true }, // Sparse because existing records might not have it initially
  status: { type: String, enum: ["completed", "delivered", "failed"], default: "completed" },
  deliveredAt: { type: Date },
}, { timestamps: true });

export const Purchase = models?.Purchase || model("Purchase", PurchaseSchema);
