"use client";

import { useEffect, useState } from "react";

interface StoreItemData {
  _id: string;
  name: string;
  description: string;
  category: string;
  price: number;
  deliveryType: string;
  deliveryData?: { type?: string; amount?: number; material?: string; command?: string };
  imageUrl?: string;
  featured: boolean;
  active: boolean;
}

interface FormData {
  name: string;
  description: string;
  category: string;
  price: number;
  deliveryType: string;
  deliveryData: { type?: string; amount?: number; command?: string };
  imageUrl?: string;
  featured: boolean;
  active: boolean;
}

const emptyItem: FormData = {
  name: "", description: "", category: "currency", price: 0,
  deliveryType: "currency", deliveryData: { type: "gems", amount: 0 },
  imageUrl: "", featured: false, active: true,
};

export default function AdminItemsPage() {
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<StoreItemData | null>(null);
  const [form, setForm] = useState<FormData>(emptyItem);

  const [items, setItems] = useState<StoreItemData[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  const fetchItems = () => {
    setLoading(true);
    fetch(`/api/admin/items?search=${encodeURIComponent(search)}&page=${page}`)
      .then((r) => r.json())
      .then((data) => {
        setItems(data.items || []);
        setTotalPages(data.totalPages || 1);
        setTotal(data.total || 0);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    const timer = setTimeout(() => fetchItems(), 300);
    return () => clearTimeout(timer);
  }, [search, page]);

  const handleSubmit = async () => {
    const method = editing ? "PUT" : "POST";
    const body = editing ? { ...form, _id: editing._id } : form;
    await fetch("/api/admin/items", {
      method, headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    setShowForm(false);
    setEditing(null);
    setForm(emptyItem);
    fetchItems();
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this item?")) return;
    await fetch("/api/admin/items", {
      method: "DELETE", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id }),
    });
    fetchItems();
  };

  const toggleField = async (id: string, field: "active" | "featured") => {
    const res = await fetch("/api/admin/items", {
      method: "PATCH", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id, field }),
    });
    const data = await res.json();
    if (data.success) {
      setItems(prev => prev.map(item =>
        item._id === id ? { ...item, [field]: data[field] } : item
      ));
    }
  };

  const openEdit = (item: StoreItemData) => {
    setEditing(item);
    setForm({
      name: item.name, description: item.description, category: item.category,
      price: item.price, deliveryType: item.deliveryType,
      deliveryData: item.deliveryData || { type: "gems", amount: 0 },
      imageUrl: item.imageUrl || "",
      featured: item.featured, active: item.active,
    });
    setShowForm(true);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">
          <span className="bg-gradient-to-r from-emerald-400 to-teal-400 bg-clip-text text-transparent">Store Items</span>
          <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{total} items</span>
        </h1>

        <div className="flex items-center gap-3">
          {/* Search */}
          <div className="relative group max-w-xs w-full">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="w-4 h-4 text-[var(--text-muted)] group-focus-within:text-emerald-400 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              placeholder="Search items..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(1); }}
              className="w-full pl-10 pr-4 py-2 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500/50 transition-all"
            />
          </div>

          <button
            onClick={() => { setShowForm(!showForm); setEditing(null); setForm(emptyItem); }}
            className="px-4 py-2 text-sm font-bold rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 text-white shadow-lg shadow-emerald-500/20 hover:shadow-emerald-500/40 transition-all shrink-0"
          >
            {showForm ? "Cancel" : "+ Add Item"}
          </button>
        </div>
      </div>

      {/* Form */}
      {showForm && (
        <div className="glass-card p-6">
          <h2 className="font-bold text-white mb-4 text-lg">{editing ? "Edit Item" : "Create New Item"}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1.5">Item Name</label>
              <input placeholder="e.g. VIP Rank" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm text-white focus:border-emerald-500/50 focus:ring-2 focus:ring-emerald-500/30 focus:outline-none transition-all" />
            </div>

            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1.5">Category</label>
              <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm text-white focus:border-emerald-500/50 focus:outline-none transition-all">
                <option value="currency">Currency</option>
                <option value="items">Items</option>
                <option value="ranks">Ranks</option>
              </select>
            </div>

            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1.5">Price (in cents)</label>
              <input placeholder="e.g. 499 = $4.99" type="number" value={form.price}
                onChange={(e) => setForm({ ...form, price: parseInt(e.target.value) || 0 })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm text-white focus:border-emerald-500/50 focus:outline-none transition-all" />
            </div>

            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1.5">Image URL (optional)</label>
              <input placeholder="https://..." value={form.imageUrl || ""} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm text-white focus:border-emerald-500/50 focus:outline-none transition-all" />
            </div>

            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1.5">Delivery Type</label>
              <select value={form.deliveryType} onChange={(e) => setForm({ ...form, deliveryType: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm text-white focus:border-emerald-500/50 focus:outline-none transition-all">
                <option value="currency">Currency</option>
                <option value="item">In-Game Item</option>
                <option value="command">Server Command (Advanced)</option>
              </select>
            </div>

            {form.deliveryType === "currency" && (
              <div>
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1.5">Currency Options</label>
                <div className="flex gap-2">
                  <select value={form.deliveryData?.type || "gems"}
                    onChange={(e) => setForm({ ...form, deliveryData: { ...form.deliveryData, type: e.target.value } })}
                    className="flex-1 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm text-white focus:border-emerald-500/50 focus:outline-none transition-all">
                    <option value="gems">Gems</option>
                    <option value="shards">Shards</option>
                  </select>
                  <input placeholder="Amount" type="number" value={form.deliveryData?.amount || 0}
                    onChange={(e) => setForm({ ...form, deliveryData: { ...form.deliveryData, amount: parseInt(e.target.value) || 0 } })}
                    className="flex-1 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm text-white focus:border-emerald-500/50 focus:outline-none transition-all" />
                </div>
              </div>
            )}

            {form.deliveryType === "command" && (
              <div>
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1.5">Command to Execute</label>
                <input placeholder="e.g. lp user {player} parent set vip" type="text" value={form.deliveryData?.command || ""}
                  onChange={(e) => setForm({ ...form, deliveryData: { ...form.deliveryData, command: e.target.value } })}
                  className="w-full bg-purple-500/10 border border-purple-500/50 rounded-xl px-4 py-2.5 text-sm text-white placeholder-[var(--text-muted)] font-mono focus:border-purple-400 focus:outline-none transition-all" />
              </div>
            )}

            <div className="md:col-span-2">
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1.5">Description</label>
              <textarea placeholder="Write a description for the store item..." value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm text-white focus:border-emerald-500/50 focus:outline-none transition-all" rows={3} />
            </div>
            <div className="flex flex-wrap items-center gap-4 md:col-span-2">
              <label className="flex items-center gap-2 text-sm text-[var(--text-secondary)]">
                <input type="checkbox" checked={form.featured} onChange={(e) => setForm({ ...form, featured: e.target.checked })} /> Featured
              </label>
              <label className="flex items-center gap-2 text-sm text-[var(--text-secondary)]">
                <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active
              </label>
              <button onClick={handleSubmit}
                className="ml-auto px-6 py-2.5 text-sm font-bold rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white transition-colors shadow-lg shadow-emerald-500/20">
                {editing ? "Save Changes" : "Create Item"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Table */}
      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)] bg-white/5">
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Item</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Category</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Price</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Delivers</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Status</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {loading && items.length === 0 ? (
                Array.from({ length: 4 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 6 }).map((_, j) => (
                      <td key={j} className="px-6 py-4"><div className="h-4 w-20 bg-white/5 rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-[var(--text-muted)] italic">No items found</td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item._id} className="hover:bg-white/5 transition-colors group">
                    <td className="px-6 py-4">
                      <span className="text-white font-bold">{item.name}</span>
                      {item.featured && <span className="ml-2 text-amber-400 text-xs bg-amber-400/10 px-2 py-0.5 rounded-full border border-amber-400/20">⭐</span>}
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-xs font-bold px-2.5 py-1 bg-white/5 rounded-lg uppercase tracking-widest text-[var(--text-secondary)] border border-white/5">{item.category}</span>
                    </td>
                    <td className="px-6 py-4 text-emerald-400 font-mono font-bold tabular-nums">${(item.price / 100).toFixed(2)}</td>
                    <td className="px-6 py-4 font-mono text-xs">
                      {item.deliveryType === 'command' ?
                        <span className="text-purple-400 font-bold bg-purple-500/10 px-2 py-1 rounded-lg border border-purple-500/20">COMMAND</span> :
                        <span className="text-[var(--text-secondary)]">{item.deliveryData?.amount || 0} {item.deliveryData?.type || item.deliveryType}</span>
                      }
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => toggleField(item._id, "active")}
                          className={`px-2.5 py-1 text-[10px] font-black uppercase tracking-widest rounded-lg border transition-all hover:scale-105 active:scale-95 ${
                            item.active
                              ? "bg-emerald-400/10 text-emerald-400 border-emerald-400/30"
                              : "bg-red-400/10 text-red-400 border-red-400/30"
                          }`}
                        >
                          {item.active ? "Active" : "Inactive"}
                        </button>
                        <button
                          onClick={() => toggleField(item._id, "featured")}
                          className={`px-2.5 py-1 text-[10px] font-black uppercase tracking-widest rounded-lg border transition-all hover:scale-105 active:scale-95 ${
                            item.featured
                              ? "bg-amber-400/10 text-amber-400 border-amber-400/30"
                              : "bg-white/5 text-[var(--text-muted)] border-white/10"
                          }`}
                        >
                          {item.featured ? "⭐ Featured" : "Normal"}
                        </button>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex gap-2 justify-end opacity-50 group-hover:opacity-100 transition-opacity">
                        <button onClick={() => openEdit(item)} className="text-xs font-bold px-3 py-1.5 rounded-lg bg-white/5 hover:bg-[var(--accent)] text-white transition-colors border border-white/10">Edit</button>
                        <button onClick={() => handleDelete(item._id)} className="text-xs font-bold px-3 py-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-400 transition-colors border border-red-500/20">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
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
