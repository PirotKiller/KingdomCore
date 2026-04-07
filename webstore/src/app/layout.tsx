import type { Metadata } from "next";
import "./globals.css";
import Navbar from "@/components/Navbar";
import Providers from "@/components/Providers";

export const metadata: Metadata = {
  title: "KingdomStore — Premium Minecraft Store",
  description: "Buy Gems, Shards, items, and ranks for The Kingdom Minecraft server.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="antialiased">
        <Providers>
          <Navbar />
          <main className="pt-16 min-h-screen">{children}</main>
          <footer className="border-t border-[var(--border)] py-8 text-center text-sm text-[var(--text-muted)]">
            <p>© 2026 The Kingdom — All rights reserved.</p>
            <p className="mt-1">Not affiliated with Mojang AB.</p>
          </footer>
        </Providers>
      </body>
    </html>
  );
}
