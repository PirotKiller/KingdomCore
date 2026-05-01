import mongoose from "mongoose";

interface MongooseCache {
  conn: typeof mongoose | null;
  promise: Promise<typeof mongoose> | null;
  uri: string | null;
}

declare global {
  // eslint-disable-next-line no-var
  var mongooseCache: MongooseCache | undefined;
}

const cached: MongooseCache = global.mongooseCache ?? { conn: null, promise: null, uri: null };
global.mongooseCache = cached;

async function dbConnect() {
  const MONGODB_URI = process.env.MONGODB_URI;

  if (!MONGODB_URI) {
    throw new Error("Please define the MONGODB_URI environment variable inside .env.local");
  }

  // Force reconnect if URI changed
  if (cached.uri !== MONGODB_URI) {
    if (cached.conn) {
      await mongoose.disconnect();
    }
    cached.conn = null;
    cached.promise = null;
    cached.uri = MONGODB_URI;
  }

  if (cached.conn && mongoose.connection.readyState === 1) {
    return cached.conn;
  }

  if (!cached.promise) {
    const opts = {
      bufferCommands: false,
      dbName: "kingdomcore",
    };

    cached.promise = mongoose.connect(MONGODB_URI, opts).then((m) => m);
  }

  try {
    cached.conn = await cached.promise;
    console.log("✅ [mongodb] Connected successfully to Cluster!");
  } catch (e: any) {
    cached.promise = null;
    cached.conn = null;
    console.error("❌ [mongodb] Connection Failed:", e.message);
    throw e;
  }

  return cached.conn;
}

export default dbConnect;
