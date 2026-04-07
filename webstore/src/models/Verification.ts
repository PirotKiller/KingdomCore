import mongoose, { Schema, model, models } from "mongoose";

const VerificationSchema = new Schema({
  code: { type: String, required: true, unique: true },
  discordId: { type: String, required: true },
  createdAt: { type: Date, default: Date.now, expires: 300 } // TTL 5 minutes
});

export const Verification = models?.Verification || model("Verification", VerificationSchema);
