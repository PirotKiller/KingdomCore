"use client";

import { useEffect, useState } from "react";

interface PurchaseData {
  _id: string;
  discordId: string;
  minecraftUuid: string;
  itemName: string;
  price: number;
  currency: string;
  status: string;
  createdAt: string;
}

export default function AdminPurchasesPage() {
  const [purchases, setPurchases] = useState<PurchaseData[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/admin/purchases")
      .then((r) => r.json())
      .then((data) => { setPurchases(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  const statusColors: Record<string, string> = {
    completed: "bg-blue-400/10 text-blue-400",
    delivered: "bg-emerald-400/10 text-emerald-400",
    failed: "bg-red-400/10 text-red-400",
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">
        <span className="bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text text-transparent">Purchases</span>
        <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{purchases.length} recent</span>
      </h1>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                <th className="px-4 py-3 font-medium">Item</th>
                <th className="px-4 py-3 font-medium">Player</th>
                <th className="px-4 py-3 font-medium">Price</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Date</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-[var(--border)]">
                    {Array.from({ length: 5 }).map((_, j) => (
                      <td key={j} className="px-4 py-3"><div className="h-4 w-20 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              ) : purchases.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-10 text-center text-[var(--text-muted)]">No purchases yet</td>
                </tr>
              ) : (
                purchases.map((p) => (
                  <tr key={p._id} className="border-b border-[var(--border)] hover:bg-[var(--bg-card-hover)] transition-colors">
                    <td className="px-4 py-3 text-white font-medium">{p.itemName}</td>
                    <td className="px-4 py-3 text-[var(--text-secondary)] font-mono text-xs">{p.minecraftUuid?.slice(0, 8)}...</td>
                    <td className="px-4 py-3 text-emerald-400">${(p.price / 100).toFixed(2)}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${statusColors[p.status] || ""}`}>
                        {p.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-[var(--text-muted)]">{new Date(p.createdAt).toLocaleString()}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
