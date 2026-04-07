"use client";

import { useState } from "react";
import useSWR from "swr";

const fetcher = (url: string) => fetch(url).then((res) => res.json());

interface StoreItemData {
  _id: string;
  name: string;
  description: string;
  category: string;
  price: number;
  deliveryType: string;
  deliveryData?: { type?: string; amount?: number; material?: string };
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

  const { data: items = [], mutate, isLoading: loading } = useSWR<StoreItemData[]>("/api/admin/items", fetcher, {
    refreshInterval: 2000, // Real-time 2s auto-refresh
  });

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
    mutate(); // Refresh instantly using SWR
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this item?")) return;
    await fetch("/api/admin/items", {
      method: "DELETE", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id }),
    });
    mutate(); // Refresh instantly using SWR
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
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">
          <span className="bg-gradient-to-r from-emerald-400 to-teal-400 bg-clip-text text-transparent">Store Items</span>
        </h1>
        <button
          onClick={() => { setShowForm(!showForm); setEditing(null); setForm(emptyItem); }}
          className="px-4 py-2 text-sm font-semibold rounded-lg bg-gradient-to-r from-[var(--accent)] to-purple-600 text-white"
        >
          {showForm ? "Cancel" : "+ Add Item"}
        </button>
      </div>

      {/* Form */}
      {showForm && (
        <div className="glass-card p-6 mb-6">
          <h2 className="font-semibold text-white mb-4">{editing ? "Edit Item" : "New Item"}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Item Name</label>
              <input placeholder="e.g. VIP Rank" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:border-[var(--accent)] focus:outline-none transition-colors" />
            </div>

            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Category</label>
              <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:border-[var(--accent)] focus:outline-none transition-colors">
                <option value="currency">Currency</option>
                <option value="items">Items</option>
                <option value="ranks">Ranks</option>
              </select>
            </div>

            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Price (in cents)</label>
              <input placeholder="e.g. 499 = $4.99" type="number" value={form.price}
                onChange={(e) => setForm({ ...form, price: parseInt(e.target.value) || 0 })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:border-[var(--accent)] focus:outline-none transition-colors" />
            </div>

            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Image URL (optional)</label>
              <input placeholder="https://..." value={form.imageUrl || ""} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:border-[var(--accent)] focus:outline-none transition-colors" />
            </div>

            <div>
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Delivery Type</label>
              <select value={form.deliveryType} onChange={(e) => setForm({ ...form, deliveryType: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:border-[var(--accent)] focus:outline-none transition-colors">
                <option value="currency">Currency</option>
                <option value="item">In-Game Item</option>
                <option value="command">Server Command (Advanced)</option>
              </select>
            </div>
            
            {form.deliveryType === "currency" && (
              <div>
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Currency Options</label>
                <div className="flex gap-2">
                  <select value={form.deliveryData?.type || "gems"}
                    onChange={(e) => setForm({ ...form, deliveryData: { ...form.deliveryData, type: e.target.value } })}
                    className="flex-1 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:border-[var(--accent)] focus:outline-none transition-colors">
                    <option value="gems">Gems</option>
                    <option value="shards">Shards</option>
                  </select>
                  <input placeholder="Amount" type="number" value={form.deliveryData?.amount || 0}
                    onChange={(e) => setForm({ ...form, deliveryData: { ...form.deliveryData, amount: parseInt(e.target.value) || 0 } })}
                    className="flex-1 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:border-[var(--accent)] focus:outline-none transition-colors" />
                </div>
              </div>
            )}

            {form.deliveryType === "command" && (
              <div>
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Command to Execute</label>
                <input placeholder="e.g. lp user {player} parent set vip" type="text" value={form.deliveryData?.command || ""}
                  onChange={(e) => setForm({ ...form, deliveryData: { ...form.deliveryData, command: e.target.value } })}
                  className="w-full bg-purple-500/10 border border-purple-500/50 rounded-lg px-4 py-2.5 text-sm text-white placeholder-[var(--text-muted)] font-mono focus:border-purple-400 focus:outline-none transition-colors" />
              </div>
            )}

            <div className="md:col-span-2">
              <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Description</label>
              <textarea placeholder="Write a description for the store item..." value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:border-[var(--accent)] focus:outline-none transition-colors" rows={3} />
            </div>
            <div className="flex flex-wrap items-center gap-4 md:col-span-2">
              <label className="flex items-center gap-2 text-sm text-[var(--text-secondary)]">
                <input type="checkbox" checked={form.featured} onChange={(e) => setForm({ ...form, featured: e.target.checked })} /> Featured
              </label>
              <label className="flex items-center gap-2 text-sm text-[var(--text-secondary)]">
                <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active
              </label>
              <button onClick={handleSubmit}
                className="ml-auto px-5 py-2 text-sm font-semibold rounded-lg bg-emerald-500 hover:bg-emerald-600 text-white transition-colors">
                {editing ? "Save Changes" : "Create Item"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Desktop Table View */}
      <div className="hidden md:block glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Item</th>
                <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Category</th>
                <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Price</th>
                <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Delivers</th>
                <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Status</th>
                <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px] text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {loading && items.length === 0 ? (
                <tr><td colSpan={6} className="px-5 py-4 text-center text-[var(--text-muted)]">Loading items...</td></tr>
              ) : items.map((item) => (
                <tr key={item._id} className="hover:bg-[var(--bg-card-hover)] transition-colors group">
                  <td className="px-5 py-4">
                    <span className="text-white font-bold text-base">{item.name}</span>
                    {item.featured && <span className="ml-2 text-amber-400 text-xs bg-amber-400/10 px-2 py-0.5 rounded-full">⭐ Featured</span>}
                  </td>
                  <td className="px-5 py-4">
                    <span className="text-xs font-semibold px-2 py-1 bg-[var(--bg-secondary)] rounded-full uppercase tracking-widest text-[var(--text-secondary)]">{item.category}</span>
                  </td>
                  <td className="px-5 py-4 text-emerald-400 font-bold">${(item.price / 100).toFixed(2)}</td>
                  <td className="px-5 py-4 font-mono text-[10px] text-[var(--text-secondary)]">
                    {item.deliveryType === 'command' ? 
                      <span className="text-purple-400 font-bold bg-purple-500/10 px-2 py-1 rounded border border-purple-500/20">{item.deliveryType}</span> : 
                      `${item.deliveryData?.amount || 0} ${item.deliveryData?.type || item.deliveryType}`
                    }
                  </td>
                  <td className="px-5 py-4">
                    <span className={`px-2 py-0.5 text-[10px] font-bold uppercase tracking-widest rounded-full ${item.active ? "bg-emerald-400/10 text-emerald-400 border border-emerald-500/20" : "bg-red-400/10 text-red-400 border border-red-500/20"}`}>
                      {item.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <div className="flex gap-2 justify-end">
                      <button onClick={() => openEdit(item)} className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-[var(--bg-secondary)] hover:bg-[var(--accent)] text-white transition-colors border border-[var(--border)]">Edit</button>
                      <button onClick={() => handleDelete(item._id)} className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-500 transition-colors border border-red-500/20">Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Mobile Card Grid View */}
      <div className="grid grid-cols-1 gap-4 md:hidden">
        {loading && items.length === 0 ? (
          <div className="text-center text-[var(--text-muted)] py-8">Loading items...</div>
        ) : items.map((item) => (
          <div key={item._id} className="glass-card p-5 space-y-4">
            <div className="flex justify-between items-start">
              <div>
                <h3 className="font-bold text-white text-lg flex items-center gap-2">
                  {item.name}
                  {item.featured && <span className="text-amber-400 text-xs bg-amber-400/10 px-2 py-0.5 rounded-full border border-amber-400/20">⭐</span>}
                </h3>
                <p className="text-[10px] text-[var(--text-muted)] font-mono uppercase tracking-widest mt-1">{item.category}</p>
              </div>
              <span className={`px-2 py-0.5 text-[10px] font-bold uppercase tracking-widest rounded-full ${item.active ? "bg-emerald-400/10 text-emerald-400 border border-emerald-500/20" : "bg-red-400/10 text-red-400 border border-red-500/20"}`}>
                {item.active ? "Active" : "Inact"}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-4 pt-2 border-t border-[var(--border)]/50">
              <div>
                <p className="text-[10px] text-[var(--text-muted)] uppercase mb-1">Price</p>
                <div className="bg-emerald-500/10 text-emerald-400 px-3 py-1.5 rounded-xl border border-emerald-500/20 font-bold w-max">
                  ${(item.price / 100).toFixed(2)}
                </div>
              </div>
              <div>
                <p className="text-[10px] text-[var(--text-muted)] uppercase mb-1">Delivers</p>
                <div className={`px-3 py-1.5 rounded-xl border font-bold text-xs w-max ${item.deliveryType === 'command' ? 'bg-purple-500/10 border-purple-500/20 text-purple-400' : 'bg-[var(--bg-secondary)] border-[var(--border)] text-white'}`}>
                  {item.deliveryType === 'command' ? 'COMMAND' : `${item.deliveryData?.amount || 0} ${item.deliveryData?.type || item.deliveryType}`}
                </div>
              </div>
            </div>

            <div className="flex gap-2 pt-2">
              <button 
                onClick={() => openEdit(item)} 
                className="flex-1 px-4 py-2 rounded-lg text-sm font-semibold bg-[var(--bg-secondary)] hover:bg-[var(--accent)] text-white transition-colors border border-[var(--border)]"
              >
                Edit Item
              </button>
              <button 
                onClick={() => handleDelete(item._id)} 
                className="px-4 py-2 rounded-lg text-sm font-semibold bg-red-500/10 hover:bg-red-500/20 text-red-500 transition-colors border border-red-500/20"
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
