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
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  const fetchPurchases = (p: number) => {
    setLoading(true);
    fetch(`/api/admin/purchases?page=${p}`)
      .then((r) => r.json())
      .then((data) => {
        setPurchases(data.purchases || []);
        setTotalPages(data.totalPages || 1);
        setTotal(data.total || 0);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchPurchases(page);
  }, [page]);

  const statusColors: Record<string, string> = {
    completed: "bg-blue-400/10 text-blue-400",
    delivered: "bg-emerald-400/10 text-emerald-400",
    failed: "bg-red-400/10 text-red-400",
  };

  const updateStatus = async (purchaseId: string, nextStatus: string) => {
    try {
      const res = await fetch(`/api/admin/purchases/${purchaseId}/status`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: nextStatus })
      });
      const data = await res.json();
      if (data.success) {
        setPurchases(prev => prev.map(p => p._id === purchaseId ? { ...p, status: nextStatus } : p));
      }
    } catch (err) {
      console.error("Failed to update status", err);
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">
        <span className="bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text text-transparent">Purchases</span>
        <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{total} total transactions</span>
      </h1>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)] bg-white/5">
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Item</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Player</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Price</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Status</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {loading && purchases.length === 0 ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 5 }).map((_, j) => (
                      <td key={j} className="px-6 py-4"><div className="h-4 w-20 bg-white/5 rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              ) : purchases.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-[var(--text-muted)] italic">No purchases yet</td>
                </tr>
              ) : (
                purchases.map((p) => (
                  <tr key={p._id} className="hover:bg-white/5 transition-colors">
                    <td className="px-6 py-4 text-white font-medium">{p.itemName}</td>
                    <td className="px-6 py-4">
                      <code className="text-xs text-[var(--text-secondary)] bg-white/5 px-2 py-1 rounded border border-white/5">
                        {p.minecraftUuid?.slice(0, 13)}...
                      </code>
                    </td>
                    <td className="px-6 py-4 text-emerald-400 font-mono tracking-tighter tabular-nums text-base">
                      ${(p.price / 100).toFixed(2)}
                    </td>
                    <td className="px-6 py-4">
                      <div className="relative group/status w-fit">
                        <span className={`px-2.5 py-1 text-xs font-bold uppercase tracking-wider rounded-lg border border-current shadow-[0_0_10px_rgba(0,0,0,0.1)] transition-all cursor-default ${statusColors[p.status] || "bg-white/5 text-gray-400"}`}>
                          {p.status}
                        </span>
                        
                        {/* Hover Selector Bridge */}
                        <div className="absolute left-0 top-[80%] pt-4 hidden group-hover/status:flex flex-col z-10 w-32 animate-in fade-in slide-in-from-top-1 duration-200">
                          <div className="bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl shadow-2xl p-1 backdrop-blur-xl flex flex-col">
                          {["completed", "delivered", "failed"].map((s) => (
                            <button
                              key={s}
                              onClick={() => updateStatus(p._id, s)}
                              className={`px-3 py-2 text-[10px] font-black uppercase tracking-widest text-left rounded-lg transition-colors ${
                                p.status === s 
                                  ? "bg-white/10 text-white cursor-default" 
                                  : "text-[var(--text-muted)] hover:bg-white/5 hover:text-white"
                              }`}
                            >
                              {s}
                            </button>
                          ))}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-[var(--text-muted)] text-xs tabular-nums">
                      {new Date(p.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Control */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-[var(--border)] flex items-center justify-between bg-white/[0.02]">
            <span className="text-xs text-[var(--text-muted)]">
              Showing page {page} of {totalPages}
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1 || loading}
                className="p-2 px-4 rounded-lg bg-white/5 border border-white/10 text-[var(--text-muted)] hover:text-white disabled:opacity-30 transition-all font-black text-xs uppercase tracking-[0.2em]"
              >
                Prev
              </button>
              <button
                onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                disabled={page === totalPages || loading}
                className="p-2 px-4 rounded-lg bg-white/5 border border-white/10 text-[var(--text-muted)] hover:text-white disabled:opacity-30 transition-all font-black text-xs uppercase tracking-[0.2em]"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
