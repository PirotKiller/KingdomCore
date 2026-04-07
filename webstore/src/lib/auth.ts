import NextAuth from "next-auth";
import Discord from "next-auth/providers/discord";
import dbConnect from "./mongodb";
import { WebUser } from "@/models/WebUser";

export const { handlers, signIn, signOut, auth } = NextAuth({
  debug: true,
  providers: [
    Discord({
      clientId: process.env.AUTH_DISCORD_ID,
      clientSecret: process.env.AUTH_DISCORD_SECRET,
    }),
  ],
  callbacks: {
    async signIn({ user, account, profile }) {
      try {
        if (!account || account.provider !== "discord") return false;
        await dbConnect();
        await WebUser.findOneAndUpdate(
          { discordId: (profile as any)?.id },
          {
            discordId: (profile as any)?.id,
            discordUsername: (profile as any)?.username || user.name,
            discordAvatar: user.image,
          },
          { upsert: true, new: true }
        );
        return true;
      } catch (error) {
        console.error("[auth] signIn error:", error);
        return true; // Still allow sign-in even if DB fails
      }
    },
    async session({ session, token }) {
      if (token?.discordId) {
        (session as any).discordId = token.discordId;
        (session as any).minecraftUuid = token.minecraftUuid;
        (session as any).minecraftUsername = token.minecraftUsername;
        (session as any).isAdmin = token.isAdmin;
      }
      return session;
    },
    async jwt({ token, account, profile }) {
      if (account && profile) {
        try {
          await dbConnect();
          const webUser = await WebUser.findOne({ discordId: (profile as any).id });
          if (webUser) {
            token.discordId = webUser.discordId;
            token.minecraftUuid = webUser.minecraftUuid;
            token.minecraftUsername = webUser.minecraftUsername;
            token.isAdmin = webUser.isAdmin;
          } else {
            token.discordId = (profile as any).id;
          }
        } catch (error) {
          console.error("[auth] jwt error:", error);
          token.discordId = (profile as any).id;
        }
      }
      return token;
    },
  },
});
