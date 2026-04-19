"use client";

import { useEffect, useState } from "react";
import Pagination from "@/components/Pagination";

interface Auction {
  _id: string;
  listingId: string;
  sellerName: string;
  materialName: string;
  amount: number;
  priceShards: number;
  priceGems: number;
  expireTime: number;
}

export default function AdminAuctionsPage() {
  const [auctions, setAuctions] = useState<Auction[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<Auction | null>(null);
  const [form, setForm] = useState({ priceShards: 0, priceGems: 0 });
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  const loadAuctions = () => {
    setLoading(true);
    fetch(`/api/admin/auctions?page=${page}`)
      .then((r) => r.json())
      .then((data) => { 
        setAuctions(data.auctions || []); 
        setTotalPages(data.totalPages || 1);
        setTotal(data.total || 0);
        setLoading(false); 
      })
      .catch(() => setLoading(false));
  };

  useEffect(loadAuctions, [page]);

  const handleEdit = (auction: Auction) => {
    setEditing(auction);
    setForm({ priceShards: auction.priceShards, priceGems: auction.priceGems });
  };

  const handleSave = async () => {
    if (!editing) return;
    await fetch("/api/admin/auctions", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ listingId: editing.listingId, ...form }),
    });
    setEditing(null);
    loadAuctions();
  };

  const handleDelete = async (listingId: string) => {
    if (!confirm("Are you sure you want to forcibly remove this auction?")) return;
    await fetch("/api/admin/auctions", {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ listingId }),
    });
    loadAuctions();
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">
        <span className="bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent">Live Auctions</span>
        <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{total} active listings</span>
      </h1>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                <th className="px-4 py-3 font-medium">Item</th>
                <th className="px-4 py-3 font-medium">Seller</th>
                <th className="px-4 py-3 font-medium">Shards Price</th>
                <th className="px-4 py-3 font-medium">Gems Price</th>
                <th className="px-4 py-3 font-medium">Expires In</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-[var(--border)]">
                    <td className="px-4 py-3"><div className="h-4 w-32 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-24 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-16 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-16 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-24 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-12 bg-[var(--bg-secondary)] rounded animate-pulse ml-auto" /></td>
                  </tr>
                ))
              ) : auctions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-[var(--text-muted)]">
                    No active auctions found.
                  </td>
                </tr>
              ) : (
                auctions.map((auction) => {
                  const timeLeftMs = auction.expireTime - Date.now();
                  const isExpired = timeLeftMs <= 0;

                  const formatTime = (ms: number) => {
                    const days = Math.floor(ms / (1000 * 60 * 60 * 24));
                    const hours = Math.floor((ms % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                    const minutes = Math.floor((ms % (1000 * 60 * 60)) / (1000 * 60));
                    
                    if (days > 0) return `${days}d ${hours}h ${minutes}m`;
                    if (hours > 0) return `${hours}h ${minutes}m`;
                    return `${minutes}m`;
                  };

                  return (
                    <tr key={auction._id} className="border-b border-[var(--border)] hover:bg-[var(--bg-card-hover)] transition-colors">
                      <td className="px-4 py-3">
                        <span className="font-medium text-white">{auction.materialName}</span>
                        <span className="ml-2 text-xs text-[var(--text-secondary)]">x{auction.amount}</span>
                      </td>
                      <td className="px-4 py-3 text-[var(--text-secondary)]">
                        {auction.sellerName}
                      </td>
                      <td className="px-4 py-3 text-emerald-400 font-medium">
                        {auction.priceShards || 0}
                      </td>
                      <td className="px-4 py-3 text-cyan-400 font-medium">
                        {auction.priceGems || 0}
                      </td>
                      <td className="px-4 py-3">
                        {isExpired ? (
                          <span className="text-red-400 text-xs font-semibold px-2 py-0.5 rounded-full bg-red-400/10 border border-red-400/20">Expired</span>
                        ) : (
                          <span className="text-amber-400 text-xs font-medium tabular-nums">{formatTime(timeLeftMs)} left</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex gap-2 justify-end">
                          <button onClick={() => handleEdit(auction)} className="text-xs px-2 py-1 rounded bg-[var(--bg-secondary)] hover:bg-[var(--bg-card-hover)] border border-[var(--border)] text-white transition-colors">
                            Edit
                          </button>
                          <button onClick={() => handleDelete(auction.listingId)} className="text-xs px-2 py-1 rounded bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors">
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
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

      {/* Edit Modal */}
      {editing && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="glass-card w-full max-w-md p-6 border border-[var(--border)] overflow-y-auto max-h-[90vh]">
            <h3 className="text-xl font-bold text-white mb-4">Edit Auction Price</h3>
            <p className="text-sm text-[var(--text-muted)] mb-6">Editing <span className="text-white font-medium">{editing.materialName}</span> listed by {editing.sellerName}</p>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1">Shards Price</label>
                <input type="number" value={form.priceShards} onChange={e => setForm({...form, priceShards: Number(e.target.value)})} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded px-3 py-2 text-emerald-400 font-bold" />
              </div>
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1">Gems Price</label>
                <input type="number" value={form.priceGems} onChange={e => setForm({...form, priceGems: Number(e.target.value)})} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded px-3 py-2 text-cyan-400 font-bold" />
              </div>
            </div>

            <div className="flex gap-3 justify-end mt-8">
              <button onClick={() => setEditing(null)} className="px-4 py-2 rounded-lg text-sm font-medium hover:bg-[var(--bg-secondary)] transition-colors">Cancel</button>
              <button onClick={handleSave} className="px-4 py-2 rounded-lg text-sm font-medium bg-emerald-500 hover:bg-emerald-600 text-white transition-colors">Save Changes</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
