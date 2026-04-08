import mongoose, { Schema, model, models } from "mongoose";

const PendingCommandSchema = new Schema({
  command: { type: String, required: true },
  uuid: { type: String, required: true, default: "CONSOLE" },
  executed: { type: Boolean, default: false },
}, { timestamps: true });

export const PendingCommand = models?.PendingCommand || model("PendingCommand", PendingCommandSchema, "web_commands");
