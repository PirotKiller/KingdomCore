"use client";

import { useEffect, useState } from "react";
import useSWR from "swr";

const fetcher = (url: string) => fetch(url).then((r) => r.json());

interface Stats {
  totalUsers: number;
  verifiedUsers: number;
  totalInGamePlayers: number;
  onlinePlayers: number;
  totalPurchases: number;
  totalRevenue: number;
  totalBounty: number;
  activeItems: number;
  activeAuctions: number;
  recentPurchases: any[];
}

export default function AdminDashboard() {
  const { data: stats, error } = useSWR<Stats>("/api/admin/stats", fetcher, {
    refreshInterval: 2000,
  });

  const cards = stats
    ? [
        { label: "Online Players", value: stats.onlinePlayers, icon: "🟢", color: "text-emerald-400" },
        { label: "Total Players", value: stats.totalInGamePlayers, icon: "🎮", color: "text-blue-400" },
        { label: "Total Revenue", value: `$${(stats.totalRevenue / 100).toFixed(2)}`, icon: "💰", color: "text-amber-400" },
        { label: "Total Purchases", value: stats.totalPurchases, icon: "💳", color: "text-indigo-400" },
        { label: "Active Auctions", value: stats.activeAuctions, icon: "⚖️", color: "text-orange-400" },
        { label: "Global Bounty", value: stats.totalBounty, icon: "🎯", color: "text-red-400" },
        { label: "Active Store Items", value: stats.activeItems, icon: "🏪", color: "text-pink-400" },
        { label: "Total Web Users", value: stats.totalUsers, icon: "👥", color: "text-purple-400" },
      ]
    : [];

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">
        <span className="bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent">Dashboard</span>
      </h1>

      {!stats ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="glass-card p-6 h-28 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {cards.map((card) => (
            <div key={card.label} className="glass-card p-5 hover:border-[var(--accent)] transition-colors border border-[var(--border)]">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xl">{card.icon}</span>
                <span className={`text-xl font-bold ${card.color}`}>{card.value}</span>
              </div>
              <div className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">{card.label}</div>
            </div>
          ))}
        </div>
      )}

      {/* Recent Purchases Section */}
      <h2 className="text-xl font-bold mt-10 mb-6 flex items-center gap-2">
        <span>🕒</span>
        <span className="text-white">Recent Purchases</span>
      </h2>
      
      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)] bg-[var(--bg-secondary)]">
                <th className="px-5 py-3 font-medium">Customer</th>
                <th className="px-5 py-3 font-medium">Item</th>
                <th className="px-5 py-3 font-medium text-right">Price</th>
                <th className="px-5 py-3 font-medium">Date</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {!stats || stats.recentPurchases.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-5 py-10 text-center text-[var(--text-muted)]">No recent purchases found.</td>
                </tr>
              ) : (
                stats.recentPurchases.map((p) => {
                  const customerName = p.userId?.minecraftUsername || p.userId?.discordUsername || "Unknown User";
                  return (
                    <tr key={p._id} className="border-b border-[var(--border)] hover:bg-[var(--bg-card-hover)] transition-colors">
                      <td className="px-5 py-3 text-white font-medium">{customerName}</td>
                      <td className="px-5 py-3 text-[var(--text-secondary)]">{p.itemName}</td>
                      <td className="px-5 py-3 text-right text-emerald-400 font-bold">${(p.price / 100).toFixed(2)}</td>
                      <td className="px-5 py-3 text-[var(--text-muted)]">{new Date(p.createdAt).toLocaleDateString()}</td>
                      <td className="px-5 py-3">
                        <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                          p.status === 'delivered' ? 'bg-emerald-400/10 text-emerald-400' : 'bg-amber-400/10 text-amber-400'
                        }`}>
                          {p.status}
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
