"use client";

import { useSession, signIn, signOut } from "next-auth/react";
import Link from "next/link";
import { useState } from "react";

export default function Navbar() {
  const { data: session, status } = useSession();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <nav className="fixed top-0 w-full z-50 border-b border-[var(--border)] bg-[var(--bg-primary)]/80 backdrop-blur-xl">
      <div className="max-w-full mx-auto px-6 sm:px-10 lg:px-16">
        <div className="flex items-center justify-between h-20">
          {/* Mobile menu button */}
          <div className="md:hidden flex items-center">
            <button
              onClick={() => setMobileOpen(!mobileOpen)}
              className="text-[var(--text-secondary)] hover:text-white p-2"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {mobileOpen ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                )}
              </svg>
            </button>
          </div>

          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[var(--accent)] to-purple-800 flex items-center justify-center text-white font-bold text-sm">
              K
            </div>
            <span className="text-lg font-bold bg-gradient-to-r from-purple-400 to-purple-200 bg-clip-text text-transparent">
              KingdomStore
            </span>
          </Link>

          {/* Desktop links */}
          <div className="hidden md:flex items-center gap-6">
            <Link href="/store" className="text-[var(--text-secondary)] hover:text-white transition-colors text-sm font-medium">
              Store
            </Link>
            {session && (
              <Link href="/account" className="text-[var(--text-secondary)] hover:text-white transition-colors text-sm font-medium">
                Account
              </Link>
            )}
            {(session as any)?.isAdmin && (
              <Link href="/admin" className="text-amber-400 hover:text-amber-300 transition-colors text-sm font-medium">
                Admin
              </Link>
            )}
          </div>

          {/* Auth */}
          <div className="flex items-center gap-3">
            {status === "loading" ? (
              <div className="h-8 w-20 bg-[var(--bg-card)] rounded-lg animate-pulse" />
            ) : session ? (
              <div className="flex items-center gap-3">
                <img
                  src={session.user?.image || ""}
                  alt=""
                  className="w-8 h-8 rounded-full border border-[var(--border)] hidden xs:block"
                />
                <span className="hidden lg:inline text-sm text-[var(--text-secondary)]">
                  {session.user?.name}
                </span>
                <button
                  onClick={() => signOut()}
                  className="text-xs bg-[var(--bg-card)] hover:bg-[var(--bg-card-hover)] border border-[var(--border)] px-3 py-1.5 rounded-lg transition-colors"
                >
                  Sign Out
                </button>
              </div>
            ) : (
              <button
                onClick={() => signIn("discord")}
                className="flex items-center gap-2 bg-[#5865F2] hover:bg-[#4752C4] text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors whitespace-nowrap"
              >
                <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z"/>
                </svg>
                <span className="hidden sm:inline">Login with Discord</span>
                <span className="sm:hidden">Login</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Mobile menu content */}
      <div className={`md:hidden absolute top-20 left-0 w-full transition-all duration-500 ease-in-out z-40 ${mobileOpen ? "opacity-100 translate-y-0" : "opacity-0 -translate-y-4 pointer-events-none"}`}>
        <div className="mx-4 p-4 rounded-2xl bg-[var(--bg-secondary)] border border-[var(--border)] shadow-2xl backdrop-blur-xl">
          <div className="flex flex-col gap-2">
            <Link 
              href="/store" 
              onClick={() => setMobileOpen(false)}
              className="flex items-center gap-3 px-4 py-3 rounded-xl text-[var(--text-secondary)] hover:text-white hover:bg-white/5 transition-all font-medium"
            >
              <span className="text-lg">🏪</span> Store
            </Link>
            {session && (
              <Link 
                href="/account" 
                onClick={() => setMobileOpen(false)}
                className="flex items-center gap-3 px-4 py-3 rounded-xl text-[var(--text-secondary)] hover:text-white hover:bg-white/5 transition-all font-medium"
              >
                <span className="text-lg">👤</span> Account
              </Link>
            )}
            {(session as any)?.isAdmin && (
              <Link 
                href="/admin" 
                onClick={() => setMobileOpen(false)}
                className="flex items-center gap-3 px-4 py-3 rounded-xl text-amber-400 hover:text-amber-300 hover:bg-amber-400/5 transition-all font-medium border border-amber-400/10"
              >
                <span className="text-lg">🛡️</span> Admin Panel
              </Link>
            )}

            {!session && status !== "loading" && (
              <button
                onClick={() => signIn("discord")}
                className="flex items-center justify-center gap-3 bg-[#5865F2] hover:bg-[#4752C4] text-white font-bold px-4 py-4 rounded-xl transition-all mt-2 shadow-lg shadow-indigo-500/20"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z"/>
                </svg>
                Login with Discord
              </button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
