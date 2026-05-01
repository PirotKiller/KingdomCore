"use client";

import { useSession } from "next-auth/react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const navItems = [
  { href: "/admin", label: "Dashboard", icon: "📊" },
  { href: "/admin/users", label: "Users", icon: "👥" },
  { href: "/admin/items", label: "Store Items", icon: "🏪" },
  { href: "/admin/purchases", label: "Purchases", icon: "💳" },
  { href: "/admin/players", label: "Players", icon: "🎮" },
  { href: "/admin/auctions", label: "Auctions", icon: "⚖️" },
  { href: "/admin/shops", label: "Server Shops", icon: "🛒" },
  { href: "/admin/moderation", label: "Moderation", icon: "🛡️" },
  { href: "/admin/logs", label: "Game Logs", icon: "📜" },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { data: session } = useSession();
  const pathname = usePathname();

  if (!(session as any)?.isAdmin) {
    return (
      <div className="max-w-xl mx-auto px-4 py-24 text-center">
        <div className="glass-card p-10">
          <div className="text-5xl mb-6">🚫</div>
          <h1 className="text-2xl font-bold text-white mb-4">Access Denied</h1>
          <p className="text-[var(--text-secondary)]">You don&apos;t have admin permissions.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col md:flex-row min-h-[calc(100vh-4rem)]">
      {/* Mobile Navigation - Sleek Scrollbar */}
      <div className="md:hidden border-b border-white/5 bg-[var(--bg-secondary)] overflow-x-auto no-scrollbar sticky top-0 z-40">
        <nav className="flex px-4 py-4 gap-2 min-w-max">
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-2.5 px-4 py-2 rounded-xl text-[11px] font-black uppercase tracking-widest transition-all ${
                  isActive
                    ? "bg-[var(--accent)] text-white shadow-lg shadow-purple-500/25"
                    : "text-[var(--text-secondary)] hover:text-white hover:bg-white/5 border border-white/5"
                }`}
              >
                <span className="text-sm">{item.icon}</span>
                {item.label}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Sidebar (Desktop) */}
      <aside className="w-60 border-r border-[var(--border)] bg-[var(--bg-secondary)] p-4 hidden md:block">
        <div className="text-xs font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4 px-3">
          Admin Panel
        </div>
        <nav className="space-y-1">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                pathname === item.href
                  ? "bg-[var(--accent)]/10 text-[var(--accent)] border border-[var(--accent)]/20"
                  : "text-[var(--text-secondary)] hover:text-white hover:bg-[var(--bg-card)]"
              }`}
            >
              <span>{item.icon}</span>
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>

      {/* Content */}
      <div className="flex-1 p-6 lg:p-8">{children}</div>
    </div>
  );
}
