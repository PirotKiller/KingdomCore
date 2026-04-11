"use client";

import { useEffect, useState } from "react";
import Pagination from "@/components/Pagination";

interface PurchaseData {
  _id: string;
  discordId: string;
  minecraftUuid: string;
  userId?: {
    minecraftUsername?: string;
  };
  itemName: string;
  price: number;
  currency: string;
  status: string;
  transactionId?: string;
  createdAt: string;
}

export default function AdminPurchasesPage() {
  const [purchases, setPurchases] = useState<PurchaseData[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  const fetchPurchases = (p: number) => {
    setLoading(true);
    fetch(`/api/admin/purchases?page=${p}&search=${encodeURIComponent(search)}`)
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
    const timer = setTimeout(() => {
        fetchPurchases(page);
    }, 300);
    return () => clearTimeout(timer);
  }, [page, search]);

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
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">
          <span className="bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text text-transparent">Purchases</span>
          <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{total} total transactions</span>
        </h1>

        <div className="relative group max-w-md w-full">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg className="w-4 h-4 text-[var(--text-muted)] group-focus-within:text-blue-400 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <input
            type="text"
            placeholder="Search Order ID, Item, or Player..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            className="w-full pl-10 pr-4 py-2 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all"
          />
        </div>
      </div>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)] bg-white/5">
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Transaction ID</th>
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
                  <td colSpan={6} className="px-6 py-12 text-center text-[var(--text-muted)] italic">No purchases yet</td>
                </tr>
              ) : (
                purchases.map((p) => (
                  <tr key={p._id} className="hover:bg-white/5 transition-colors">
                    <td className="px-6 py-4">
                      <span className="text-xs font-mono text-blue-400 font-bold bg-blue-400/5 px-2 py-1 rounded border border-blue-400/20">
                        {p.transactionId || "LEGACY"}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-white font-medium">{p.itemName}</td>
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="text-white font-medium">{p.userId?.minecraftUsername || "Unknown Player"}</span>
                        <span className="text-[10px] text-[var(--text-muted)] font-mono">{p.minecraftUuid}</span>
                        <span className="text-[10px] text-blue-400/70">Discord: {p.discordId}</span>
                      </div>
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

        <Pagination
          page={page}
          total={total}
          totalPages={totalPages}
          limit={20}
          onPageChange={setPage}
          loading={loading}
        />
      </div>
    </div>
  );
}
