const mongoose = require("mongoose");

const MONGODB_URI = "mongodb://thakurdhruv104_db_user:MKPadVLcsvn2iDEr@ac-romgn9j-shard-00-00.kjnj5bn.mongodb.net:27017,ac-romgn9j-shard-00-01.kjnj5bn.mongodb.net:27017,ac-romgn9j-shard-00-02.kjnj5bn.mongodb.net:27017/kingdomcore?tls=true&authSource=admin&directConnection=false&retryWrites=true&w=majority&appName=Cluster0";

async function checkDb() {
  await mongoose.connect(MONGODB_URI);
  console.log("Connected to MongoDB.");

  const db = mongoose.connection.db;

  const collections = await db.listCollections().toArray();
  console.log("Collections:", collections.map(c => c.name));

  const shopsCol = db.collection("shops");
  const shops = await shopsCol.find({}).toArray();
  console.log("\nShops Data:");
  console.log(JSON.stringify(shops, null, 2));
  
  const shopItemsCol = db.collection("shopitems");
  const shopItems = await shopItemsCol.find({}).toArray();
  console.log("\nShopItems (Old) Data:");
  console.log(JSON.stringify(shopItems, null, 2));

  const webCmds = db.collection("web_commands");
  const cmds = await webCmds.find({}).toArray();
  console.log("\nWeb Commands Data:");
  console.log(JSON.stringify(cmds, null, 2));

  const pendingCmds = db.collection("pending_commands");
  const pCmds = await pendingCmds.find({}).toArray();
  console.log("\nPending Commands (Old) Data:");
  console.log(JSON.stringify(pCmds, null, 2));

  process.exit(0);
}

checkDb().catch(console.error);
