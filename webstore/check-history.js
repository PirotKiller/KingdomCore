const { MongoClient } = require('mongodb');

async function checkData() {
    const uri = "mongodb+srv://thakurdhruv104_db_user:MKPadVLcsvn2iDEr@cluster0.kjnj5bn.mongodb.net/kingdomcore?retryWrites=true&w=majority";
    const client = new MongoClient(uri);
    try {
        await client.connect();
        const db = client.db("kingdomcore");
        const playerName = "PirotKiller";
        
        console.log(`Checking data for ${playerName}...`);
        
        const pun = await db.collection("punishments").find({ playerName: { $regex: new RegExp(`^${playerName}$`, "i") } }).toArray();
        console.log(`Punishments: ${pun.length}`);
        if (pun.length > 0) console.log(JSON.stringify(pun[0], null, 2));

        const logs = await db.collection("game_logs").find({ playerName: { $regex: new RegExp(`^${playerName}$`, "i") } }).toArray();
        console.log(`Game Logs: ${logs.length}`);
        
        const players = await db.collection("players").find({ lastKnownName: { $regex: new RegExp(`^${playerName}$`, "i") } }).toArray();
        console.log(`Players: ${players.length}`);
        if (players.length > 0) console.log(JSON.stringify(players[0], null, 2));

    } catch(e) {
        console.error(e);
    } finally {
        await client.close();
    }
}

checkData();
