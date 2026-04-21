"use client";

import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { MINECRAFT_MATERIALS } from "@/lib/materials";

const SHOP_TYPES = [
  { key: "armor", label: "Armor Shop", icon: "🛡️" },
  { key: "converter", label: "Ore Converter", icon: "🔄" },
  { key: "enchant", label: "Enchantments", icon: "✨" },
  { key: "end", label: "End Shop", icon: "🌀" },
  { key: "farming", label: "Farming Shop", icon: "🌾" },
  { key: "fisherman", label: "Fisherman", icon: "🎣" },
  { key: "fletcher", label: "Fletcher", icon: "🏹" },
  { key: "nether", label: "Nether Shop", icon: "🔥" },
  { key: "potion", label: "Potions", icon: "🧪" },
  { key: "redstone", label: "Redstone Shop", icon: "⚡" },
  { key: "stone", label: "Stone Shop", icon: "🪨" },
  { key: "wood", label: "Wood Shop", icon: "🪵" },
];

const CURRENCY_INFO: Record<string, string> = {
  wood: "SHARDS", stone: "SHARDS", fisherman: "SHARDS", fletcher: "SHARDS",
  redstone: "SHARDS", farming: "SHARDS", converter: "SHARDS",
  enchant: "DUAL", potion: "DUAL",
  nether: "DUAL", end: "DUAL", armor: "DUAL",
};

interface ShopItemData {
  _id?: string;
  shopType: string;
  itemKey: string;
  name: string;
  material: string;
  amount: number;
  lore: string[];
  priceShards: number;
  priceGems: number;
  enchant: string | null;
  enchantLevel: number;
  damage: number;
  speed: number;
  class: string | null;
  tier: string | null;
  cmd: number;
  order: number;
  active: boolean;
}

const emptyItem = (shopType: string): ShopItemData => ({
  shopType,
  itemKey: "",
  name: "",
  material: "",
  amount: 1,
  lore: [],
  priceShards: 0,
  priceGems: 0,
  enchant: null,
  enchantLevel: 0,
  damage: 0,
  speed: 0,
  class: null,
  tier: null,
  cmd: 0,
  order: 0,
  active: true,
});

export default function AdminShopsPage() {
  const [allItems, setAllItems] = useState<Record<string, ShopItemData[]>>({});
  const [selectedShop, setSelectedShop] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ShopItemData | null>(null);
  const [form, setForm] = useState<ShopItemData>(emptyItem(""));
  const [saving, setSaving] = useState(false);
  const [syncing, setSyncing] = useState(false);

  // Material selection
  const [materialSearch, setMaterialSearch] = useState("");
  const [showMaterialList, setShowMaterialList] = useState(false);

  // Lore editing
  const [loreInput, setLoreInput] = useState("");

  const loadShops = async () => {
    try {
      const res = await fetch("/api/admin/shops");
      const data = await res.json();
      setAllItems(data);
    } catch {
      toast.error("Failed to load shops");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadShops(); }, []);

  const currentItems = selectedShop ? (allItems[selectedShop] || []) : [];
  const currencyMode = selectedShop ? CURRENCY_INFO[selectedShop] : "SHARDS";

  const openCreateModal = () => {
    if (!selectedShop) return;
    setEditingItem(null);
    setForm(emptyItem(selectedShop));
    setLoreInput("");
    setModalOpen(true);
  };

  const openEditModal = (item: ShopItemData) => {
    setEditingItem(item);
    setForm({ ...item });
    setLoreInput(item.lore?.join("\n") || "");
    setMaterialSearch(item.material);
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingItem(null);
    setMaterialSearch("");
    setShowMaterialList(false);
  };

  const handleSave = async () => {
    if (!form.name || !form.material) {
      toast.error("Name and Material are required");
      return;
    }

    setSaving(true);
    const payload = {
      ...form,
      lore: loreInput.split("\n").filter((l: string) => l.trim() !== ""),
    };

    try {
      if (editingItem?._id) {
        // Update
        const res = await fetch("/api/admin/shops", {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ ...payload, _id: editingItem._id }),
        });
        if (!res.ok) throw new Error();
        toast.success("Item updated!");
      } else {
        // Create
        const res = await fetch("/api/admin/shops", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error();
        toast.success("Item created!");
      }
      closeModal();
      await loadShops();
    } catch {
      toast.error("Failed to save item");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (item: ShopItemData) => {
    if (!confirm(`Delete "${item.name.replace(/§./g, "")}"?`)) return;

    try {
      const res = await fetch("/api/admin/shops", {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ _id: item._id }),
      });
      if (!res.ok) throw new Error();
      toast.success("Item deleted");
      await loadShops();
    } catch {
      toast.error("Failed to delete item");
    }
  };

  const handleToggleActive = async (item: ShopItemData) => {
    try {
      await fetch("/api/admin/shops", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ _id: item._id, active: !item.active }),
      });
      toast.success(item.active ? "Item disabled" : "Item enabled");
      await loadShops();
    } catch {
      toast.error("Failed to toggle status");
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const res = await fetch("/api/admin/shops", { method: "PATCH" });
      if (!res.ok) throw new Error();
      const data = await res.json();
      toast.success(data.message || "Sync command sent!");
    } catch {
      toast.error("Failed to send sync command");
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">
            <span className="bg-gradient-to-r from-purple-400 to-indigo-400 bg-clip-text text-transparent">In-Game Shops</span>
            <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">MongoDB Editor</span>
          </h1>
          <p className="text-sm text-[var(--text-muted)] mt-1">Manage shop items directly — changes sync to the game server.</p>
        </div>
        <button
          onClick={handleSync}
          disabled={syncing}
          className={`px-5 py-2.5 rounded-xl font-bold text-sm transition-all active:scale-95 flex items-center gap-2 border ${
            syncing 
              ? "bg-[var(--bg-secondary)] text-[var(--text-muted)] border-[var(--border)]" 
              : "bg-[var(--accent)]/10 text-[var(--accent)] border-[var(--accent)]/20 hover:bg-[var(--accent)] hover:text-white"
          }`}
        >
          <svg className={`w-4 h-4 ${syncing ? "animate-spin" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          {syncing ? "Syncing..." : "Sync to Game"}
        </button>
      </div>

      {/* Shop Type Selector */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 mb-8">
        {loading ? (
          <>
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-16 bg-[var(--bg-secondary)] rounded-xl animate-pulse border border-[var(--border)]" />
            ))}
          </>
        ) : (
          SHOP_TYPES.map(shop => {
            const isSelected = selectedShop === shop.key;
            const itemCount = allItems[shop.key]?.length || 0;
            return (
              <button
                key={shop.key}
                onClick={() => setSelectedShop(shop.key)}
                className={`flex items-center gap-3 p-3 rounded-xl transition-all duration-300 text-left border relative overflow-hidden group ${
                  isSelected
                    ? "bg-[var(--accent)]/10 border-[var(--accent)]/50 shadow-[0_0_20px_rgba(139,92,246,0.15)] text-white"
                    : "bg-[var(--bg-secondary)] border-[var(--border)] text-[var(--text-secondary)] hover:border-[var(--text-muted)] hover:bg-white/5"
                }`}
              >
                {isSelected && <div className="absolute inset-0 bg-gradient-to-r from-[var(--accent)]/20 to-transparent opacity-50" />}
                <span className="text-xl z-10">{shop.icon}</span>
                <div className="z-10 min-w-0">
                  <span className={`font-bold text-sm block truncate ${isSelected ? "text-white" : "group-hover:text-white transition-colors"}`}>
                    {shop.label}
                  </span>
                  <span className="text-[10px] text-[var(--text-muted)]">{itemCount} items</span>
                </div>
              </button>
            );
          })
        )}
      </div>

      {/* Items Table */}
      {selectedShop && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <h2 className="font-bold text-white text-lg">
                {SHOP_TYPES.find(s => s.key === selectedShop)?.label}
              </h2>
              <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-[var(--accent)]/10 text-[var(--accent)] border border-[var(--accent)]/20">
                {currencyMode}
              </span>
            </div>
            <button
              onClick={openCreateModal}
              className="px-5 py-2.5 bg-emerald-500 hover:bg-emerald-400 text-white rounded-xl font-bold text-sm transition-all active:scale-95 flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
              </svg>
              Add Item
            </button>
          </div>

          {currentItems.length === 0 ? (
            <div className="glass-card p-12 text-center">
              <div className="text-5xl mb-4 opacity-50">📦</div>
              <h3 className="text-lg font-bold text-white">No items yet</h3>
              <p className="text-sm text-[var(--text-muted)] mt-1">Click "Add Item" to create your first shop item.</p>
            </div>
          ) : (
            <div className="glass-card overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                    <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Item</th>
                    <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Material</th>
                    <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Qty</th>
                    <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Pricing</th>
                    <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Status</th>
                    <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px] text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {currentItems.map((item: ShopItemData) => (
                    <tr key={item._id} className="hover:bg-[var(--bg-card-hover)] transition-colors group">
                      <td className="px-5 py-4">
                        <div className="font-bold text-white group-hover:text-[var(--accent)] transition-colors">
                          {item.name.replace(/§./g, "")}
                        </div>
                        <div className="text-[10px] text-[var(--text-muted)] font-mono">{item.itemKey}</div>
                      </td>
                      <td className="px-5 py-4">
                        <code className="text-xs bg-[var(--bg-secondary)] px-1.5 py-0.5 rounded border border-[var(--border)]">
                          {item.material}
                        </code>
                      </td>
                      <td className="px-5 py-4 text-[var(--text-secondary)] font-medium">{item.amount}</td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap gap-1.5">
                          {item.priceShards > 0 && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-bold">
                              {item.priceShards} Shards
                            </span>
                          )}
                          {item.priceGems > 0 && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 font-bold">
                              {item.priceGems} Gems
                            </span>
                          )}
                          {item.priceShards === 0 && item.priceGems === 0 && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-gray-500/10 text-gray-400 border border-gray-500/20">Free</span>
                          )}
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <button
                          onClick={() => handleToggleActive(item)}
                          className={`text-[10px] px-2.5 py-1 rounded-full font-bold transition-colors ${
                            item.active
                              ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 hover:bg-red-500/10 hover:text-red-400 hover:border-red-500/20"
                              : "bg-red-500/10 text-red-400 border border-red-500/20 hover:bg-emerald-500/10 hover:text-emerald-400 hover:border-emerald-500/20"
                          }`}
                        >
                          {item.active ? "Active" : "Disabled"}
                        </button>
                      </td>
                      <td className="px-5 py-4 text-right">
                        <div className="flex items-center gap-2 justify-end">
                          <button
                            onClick={() => openEditModal(item)}
                            className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-[var(--bg-secondary)] hover:bg-[var(--accent)] hover:text-white transition-all border border-[var(--border)] hover:border-[var(--accent)]"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDelete(item)}
                            className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-[var(--bg-secondary)] hover:bg-red-500 hover:text-white transition-all border border-[var(--border)] hover:border-red-500"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Create/Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-end md:items-center justify-center p-0 md:p-4 z-50">
          <div className="glass-card w-full md:max-w-2xl p-6 md:p-8 border-x-0 md:border border-[var(--border)] overflow-y-auto max-h-[95vh] rounded-t-3xl md:rounded-2xl pb-12 md:pb-8">
            <div className="flex justify-between items-center mb-6">
              <div>
                <h3 className="text-2xl font-bold text-white">
                  {editingItem ? "Edit Shop Item" : "Add New Item"}
                </h3>
                <p className="text-sm text-[var(--text-muted)] mt-1">
                  {SHOP_TYPES.find(s => s.key === form.shopType)?.label || form.shopType}
                </p>
              </div>
              <button onClick={closeModal} className="p-2 bg-[var(--bg-secondary)] rounded-full text-[var(--text-secondary)] hover:text-white transition-colors">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              {/* Name */}
              <div className="md:col-span-2">
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-2">Display Name</label>
                <input
                  type="text"
                  value={form.name}
                  onChange={e => setForm({ ...form, name: e.target.value })}
                  placeholder="e.g. §6Golden Shovel"
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-3 text-white focus:outline-none focus:border-[var(--accent)] transition-colors text-lg"
                />
              </div>

              {/* Material Dropdown */}
              <div className="relative">
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-2">Material ID</label>
                <div className="relative">
                  <input
                    type="text"
                    value={materialSearch}
                    onChange={e => {
                      const val = e.target.value.toUpperCase();
                      setMaterialSearch(val);
                      setForm({ ...form, material: val });
                      setShowMaterialList(true);
                    }}
                    onFocus={() => setShowMaterialList(true)}
                    placeholder="Search material... (e.g. DIAMOND)"
                    className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-3 text-[var(--text-secondary)] focus:outline-none focus:border-[var(--accent)] font-mono pr-10"
                  />
                  <div className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                  </div>
                </div>

                {showMaterialList && (
                  <div className="absolute left-0 right-0 mt-2 bg-[var(--bg-card)] border border-[var(--border)] rounded-xl shadow-2xl z-[60] overflow-hidden backdrop-blur-xl">
                    <div className="max-h-60 overflow-y-auto">
                      {MINECRAFT_MATERIALS.filter(m => 
                        m.id.toLowerCase().includes(materialSearch.toLowerCase()) || 
                        m.name.toLowerCase().includes(materialSearch.toLowerCase())
                      ).slice(0, 50).map((m) => (
                        <button
                          key={m.id}
                          onClick={() => {
                            setMaterialSearch(m.id);
                            setForm({ ...form, material: m.id });
                            setShowMaterialList(false);
                          }}
                          className="w-full text-left px-4 py-2.5 hover:bg-[var(--accent)]/10 flex items-center justify-between transition-colors border-b border-[var(--border)] last:border-0 group"
                        >
                          <div>
                            <div className="text-sm font-bold text-white group-hover:text-[var(--accent)]">{m.name}</div>
                            <div className="text-[10px] text-[var(--text-muted)] font-mono">{m.id}</div>
                          </div>
                          <div className="text-xs text-[var(--text-muted)] opacity-0 group-hover:opacity-100 transition-opacity">Select</div>
                        </button>
                      ))}
                      {MINECRAFT_MATERIALS.filter(m => 
                        m.id.toLowerCase().includes(materialSearch.toLowerCase()) || 
                        m.name.toLowerCase().includes(materialSearch.toLowerCase())
                      ).length === 0 && (
                        <div className="p-4 text-center text-sm text-[var(--text-muted)] italic">
                          No matching materials found.
                        </div>
                      )}
                    </div>
                  </div>
                )}
                {/* Click outside to close */}
                {showMaterialList && (
                  <div 
                    className="fixed inset-0 z-50 pointer-events-auto" 
                    onClick={() => setShowMaterialList(false)}
                  />
                )}
              </div>

              {/* Quantity */}
              <div>
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-2">Quantity</label>
                <input
                  type="number"
                  value={form.amount}
                  onChange={e => setForm({ ...form, amount: Number(e.target.value) })}
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-3 text-white focus:outline-none focus:border-[var(--accent)] font-bold"
                />
              </div>

              {/* Lore */}
              <div className="md:col-span-2">
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-2">Lore (one line per row)</label>
                <textarea
                  value={loreInput}
                  onChange={e => setLoreInput(e.target.value)}
                  placeholder={"§7A powerful weapon\n§7forged in fire"}
                  rows={3}
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-3 text-[var(--text-secondary)] focus:outline-none focus:border-[var(--accent)] font-mono text-sm resize-none"
                />
              </div>

              {/* Pricing Section */}
              <div className="md:col-span-2 pt-4 border-t border-[var(--border)] mt-1">
                <h4 className="text-sm font-bold text-white mb-4 flex items-center gap-2">
                  <svg className="w-4 h-4 text-emerald-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4 4a2 2 0 00-2 2v4a2 2 0 002 2V6h10a2 2 0 00-2-2H4zm2 6a2 2 0 012-2h8a2 2 0 012 2v4a2 2 0 01-2 2H8a2 2 0 01-2-2v-4zm6 4a2 2 0 100-4 2 2 0 000 4z" clipRule="evenodd" />
                  </svg>
                  Currency & Pricing
                  <span className="text-[10px] text-[var(--text-muted)] ml-2">({currencyMode})</span>
                </h4>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {(currencyMode === "SHARDS" || currencyMode === "DUAL") && (
                    <div className="bg-emerald-500/5 p-4 rounded-2xl border border-emerald-500/10">
                      <label className="block text-xs font-bold text-emerald-400/70 mb-2 uppercase tracking-tighter">
                        {selectedShop === "converter" ? "Sell Reward (Shards)" : (currencyMode === "DUAL" ? "Shard Price" : "Price (Shards)")}
                      </label>
                      <input
                        type="number"
                        value={form.priceShards}
                        onChange={e => setForm({ ...form, priceShards: Number(e.target.value) })}
                        className="w-full bg-transparent border-none p-0 text-xl text-emerald-400 font-black focus:outline-none"
                      />
                    </div>
                  )}
                  {currencyMode === "DUAL" && (
                    <div className="bg-cyan-500/5 p-4 rounded-2xl border border-cyan-500/10">
                      <label className="block text-xs font-bold text-cyan-400/70 mb-2 uppercase tracking-tighter">
                        {selectedShop === "converter" ? "Sell Reward (Gems)" : "Gem Price"}
                      </label>
                      <input
                        type="number"
                        value={form.priceGems}
                        onChange={e => setForm({ ...form, priceGems: Number(e.target.value) })}
                        className="w-full bg-transparent border-none p-0 text-xl text-cyan-400 font-black focus:outline-none"
                      />
                    </div>
                  )}
                </div>
              </div>

              {/* Advanced Fields */}
              <div className="md:col-span-2 pt-4 border-t border-[var(--border)] mt-1">
                <details className="group">
                  <summary className="text-sm font-bold text-[var(--text-muted)] cursor-pointer hover:text-white transition-colors flex items-center gap-2">
                    <svg className="w-4 h-4 transition-transform group-open:rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
                    </svg>
                    Advanced Options
                  </summary>
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mt-4">
                    <div>
                      <label className="block text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Enchantment</label>
                      <input type="text" value={form.enchant || ""} onChange={e => setForm({ ...form, enchant: e.target.value || null })} placeholder="e.g. sharpness" className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[var(--accent)] font-mono" />
                    </div>
                    <div>
                      <label className="block text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Enchant Level</label>
                      <input type="number" value={form.enchantLevel} onChange={e => setForm({ ...form, enchantLevel: Number(e.target.value) })} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[var(--accent)]" />
                    </div>
                    <div>
                      <label className="block text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Custom Model Data</label>
                      <input type="number" value={form.cmd} onChange={e => setForm({ ...form, cmd: Number(e.target.value) })} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[var(--accent)]" />
                    </div>
                    <div>
                      <label className="block text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Damage</label>
                      <input type="number" step="0.1" value={form.damage} onChange={e => setForm({ ...form, damage: Number(e.target.value) })} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[var(--accent)]" />
                    </div>
                    <div>
                      <label className="block text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Speed</label>
                      <input type="number" step="0.1" value={form.speed} onChange={e => setForm({ ...form, speed: Number(e.target.value) })} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[var(--accent)]" />
                    </div>
                    <div>
                      <label className="block text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Order</label>
                      <input type="number" value={form.order} onChange={e => setForm({ ...form, order: Number(e.target.value) })} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[var(--accent)]" />
                    </div>
                    <div>
                      <label className="block text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Class</label>
                      <input type="text" value={form.class || ""} onChange={e => setForm({ ...form, class: e.target.value || null })} placeholder="WARRIOR" className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[var(--accent)] font-mono" />
                    </div>
                    <div>
                      <label className="block text-[10px] uppercase tracking-widest font-bold text-[var(--text-muted)] mb-1">Tier</label>
                      <input type="text" value={form.tier || ""} onChange={e => setForm({ ...form, tier: e.target.value || null })} placeholder="LEGENDARY" className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[var(--accent)] font-mono" />
                    </div>
                  </div>
                </details>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex flex-col md:flex-row gap-3 justify-end mt-8">
              <button
                onClick={closeModal}
                className="order-2 md:order-1 flex-1 md:flex-none px-8 py-3 rounded-xl text-sm font-bold bg-[var(--bg-secondary)] hover:bg-[var(--bg-card-hover)] text-[var(--text-secondary)] transition-all active:scale-95"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="order-1 md:order-2 flex-1 md:flex-none px-8 py-3 rounded-xl text-sm font-black bg-emerald-500 hover:bg-emerald-400 text-white shadow-lg shadow-emerald-500/20 transition-all active:scale-95 disabled:opacity-50"
              >
                {saving ? "Saving..." : editingItem ? "Update Item" : "Create Item"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
