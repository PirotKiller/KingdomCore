import mongoose, { Schema, model, models } from "mongoose";

const WebUserSchema = new Schema({
  discordId: { type: String, required: true, unique: true },
  discordUsername: { type: String, required: true },
  discordAvatar: { type: String },
  minecraftUuid: { type: String, index: true },
  minecraftUsername: { type: String },
  isAdmin: { type: Boolean, default: false },
}, { timestamps: true });

export const WebUser = models?.WebUser || model("WebUser", WebUserSchema);
