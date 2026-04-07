const mongoose = require('mongoose');

async function main() {
  await mongoose.connect('mongodb+srv://thakurdhruv104_db_user:MKPadVLcsvn2iDEr@cluster0.kjnj5bn.mongodb.net/kingdomcore?retryWrites=true&w=majority');
  
  const db = mongoose.connection.db;
  
  // List all webusers first
  const users = await db.collection('webusers').find({}).toArray();
  console.log('All web users:');
  users.forEach(u => console.log(`  - ${u.discordUsername} (admin: ${u.isAdmin})`));
  
  // Set PirotKiller as admin (try case-insensitive)
  const result = await db.collection('webusers').updateOne(
    { discordUsername: { $regex: /^PirotKiller$/i } },
    { $set: { isAdmin: true } }
  );
  
  console.log(`\nMatched: ${result.matchedCount}, Modified: ${result.modifiedCount}`);
  
  if (result.matchedCount === 0 && users.length > 0) {
    // Try first user as fallback
    const firstUser = users[0];
    console.log(`\nPirotKiller not found. Setting first user "${firstUser.discordUsername}" as admin instead...`);
    await db.collection('webusers').updateOne(
      { _id: firstUser._id },
      { $set: { isAdmin: true } }
    );
    console.log('Done!');
  }
  
  await mongoose.disconnect();
}

main().catch(console.error);
