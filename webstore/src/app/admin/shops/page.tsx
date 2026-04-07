"use client";

import { useEffect, useState } from "react";

export default function AdminShopsPage() {
  const [shops, setShops] = useState<Record<string, any>>({});
  const [selectedShopId, setSelectedShopId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Edit state
  const [editingItemKey, setEditingItemKey] = useState<string | null>(null);
  const [form, setForm] = useState<any>({});

  const loadShops = () => {
    fetch("/api/admin/shops")
      .then((r) => r.json())
      .then((data) => { setShops(data); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(loadShops, []);

  const selectedShop = selectedShopId ? shops[selectedShopId] : null;

  const handleEdit = (itemKey: string, itemData: any) => {
    setEditingItemKey(itemKey);
    setForm(JSON.parse(JSON.stringify(itemData))); // clone
  };

  const handleSaveItem = async () => {
    if (!selectedShopId || !editingItemKey) return;
    
    // clone shop config
    const newShopData = JSON.parse(JSON.stringify(selectedShop));
    newShopData.items[editingItemKey] = form;

    await fetch("/api/admin/shops", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ shopId: selectedShopId, content: newShopData }),
    });

    setEditingItemKey(null);
    loadShops();
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">
        <span className="bg-gradient-to-r from-purple-400 to-indigo-400 bg-clip-text text-transparent">In-Game Shops</span>
        <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">YAML Editor</span>
      </h1>

      <div className="flex gap-6 mb-8 overflow-x-auto pb-2">
        {loading ? (
          <div className="flex gap-2">
            <div className="h-10 w-24 bg-[var(--bg-secondary)] rounded-lg animate-pulse" />
            <div className="h-10 w-24 bg-[var(--bg-secondary)] rounded-lg animate-pulse" />
          </div>
        ) : (
          Object.keys(shops).map(shopId => (
            <button
              key={shopId}
              onClick={() => setSelectedShopId(shopId)}
              className={`px-4 py-2 text-sm font-semibold rounded-lg transition-colors whitespace-nowrap ${
                selectedShopId === shopId
                  ? "bg-[var(--accent)]/10 text-[var(--accent)] border border-[var(--accent)]/20"
                  : "bg-[var(--bg-secondary)] text-[var(--text-secondary)] hover:text-white"
              }`}
            >
              {shops[shopId].title ? String(shops[shopId].title).replace(/§./g, "") : shopId}
            </button>
          ))
        )}
      </div>

      {selectedShop && (
        <div className="space-y-6">
          <div className="flex items-center justify-between px-2">
            <h2 className="font-bold text-white text-lg">
              {selectedShop.title ? String(selectedShop.title).replace(/§./g, "") : selectedShopId}
            </h2>
            <span className="text-xs text-[var(--text-muted)] bg-[var(--bg-secondary)] px-2 py-1 rounded-full border border-[var(--border)]">
              {Object.keys(selectedShop.items || {}).length} Items
            </span>
          </div>

          {/* Desktop Table View (>= 768px) */}
          <div className="hidden md:block glass-card overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                  <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Item</th>
                  <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Material</th>
                  <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Qty</th>
                  <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px]">Pricing</th>
                  <th className="px-5 py-4 font-medium uppercase tracking-wider text-[10px] text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {Object.keys(selectedShop.items || {}).map((itemKey) => {
                  const item = selectedShop.items[itemKey];
                  return (
                    <tr key={itemKey} className="hover:bg-[var(--bg-card-hover)] transition-colors group">
                      <td className="px-5 py-4">
                        <div className="font-bold text-white group-hover:text-[var(--accent)] transition-colors">
                          {item.name ? item.name.replace(/§./g, "") : itemKey}
                        </div>
                        <div className="text-[10px] text-[var(--text-muted)] font-mono">{itemKey}</div>
                      </td>
                      <td className="px-5 py-4 text-[var(--text-secondary)]">
                        <code className="text-xs bg-[var(--bg-secondary)] px-1.5 py-0.5 rounded border border-[var(--border)]">
                          {item.material || "STONE"}
                        </code>
                      </td>
                      <td className="px-5 py-4 text-[var(--text-secondary)] font-medium">{item.amount || 1}</td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap gap-2">
                          {item.price !== undefined && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-bold">
                              {item.price} Money
                            </span>
                          )}
                          {item["price-shards"] !== undefined && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-bold">
                              {item["price-shards"]} Shards
                            </span>
                          )}
                          {item["price-gems"] !== undefined && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 font-bold">
                              {item["price-gems"]} Gems
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-5 py-4 text-right">
                        <button 
                          onClick={() => handleEdit(itemKey, item)} 
                          className="px-4 py-1.5 rounded-lg text-xs font-semibold bg-[var(--bg-secondary)] hover:bg-[var(--accent)] hover:text-white transition-all border border-[var(--border)] hover:border-[var(--accent)]"
                        >
                          Edit
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Mobile Card View (< 768px) */}
          <div className="grid grid-cols-1 gap-4 md:hidden">
            {Object.keys(selectedShop.items || {}).map((itemKey) => {
              const item = selectedShop.items[itemKey];
              return (
                <div key={itemKey} className="glass-card p-5 space-y-4 relative group">
                  <div className="flex justify-between items-start">
                    <div>
                      <h3 className="font-bold text-white text-lg leading-tight">
                        {item.name ? item.name.replace(/§./g, "") : itemKey}
                      </h3>
                      <p className="text-[10px] text-[var(--text-muted)] font-mono uppercase tracking-widest mt-1">ID: {itemKey}</p>
                    </div>
                    <button 
                      onClick={() => handleEdit(itemKey, item)} 
                      className="p-2 rounded-xl bg-[var(--accent)]/10 text-[var(--accent)] border border-[var(--accent)]/30 active:scale-95 transition-transform"
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-4 pt-2 border-t border-[var(--border)]/50">
                    <div>
                      <p className="text-[10px] text-[var(--text-muted)] uppercase mb-1">Material</p>
                      <code className="text-xs text-[var(--text-secondary)]">{item.material || "STONE"}</code>
                    </div>
                    <div>
                      <p className="text-[10px] text-[var(--text-muted)] uppercase mb-1">Amount</p>
                      <span className="text-sm text-white font-bold">{item.amount || 1}x</span>
                    </div>
                  </div>

                  <div className="pt-2">
                    <p className="text-[10px] text-[var(--text-muted)] uppercase mb-2">Current Pricing</p>
                    <div className="flex flex-wrap gap-2">
                      {item.price !== undefined && (
                        <div className="flex items-center gap-1.5 bg-emerald-500/10 text-emerald-400 px-3 py-1.5 rounded-xl border border-emerald-500/20 text-xs font-bold">
                          {item.price} <span className="text-[8px] uppercase tracking-tighter">Money</span>
                        </div>
                      )}
                      {item["price-shards"] !== undefined && (
                        <div className="flex items-center gap-1.5 bg-emerald-500/10 text-emerald-400 px-3 py-1.5 rounded-xl border border-emerald-500/20 text-xs font-bold">
                          {item["price-shards"]} <span className="text-[8px] uppercase tracking-tighter">Shards</span>
                        </div>
                      )}
                      {item["price-gems"] !== undefined && (
                        <div className="flex items-center gap-1.5 bg-cyan-500/10 text-cyan-400 px-3 py-1.5 rounded-xl border border-cyan-500/20 text-xs font-bold">
                          {item["price-gems"]} <span className="text-[8px] uppercase tracking-tighter">Gems</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {editingItemKey && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-end md:items-center justify-center p-0 md:p-4 z-50 transition-opacity">
          <div className="glass-card w-full md:max-w-xl p-6 md:p-8 border-x-0 md:border border-[var(--border)] overflow-y-auto max-h-[95vh] rounded-t-3xl md:rounded-2xl pb-12 md:pb-8">
            <div className="flex justify-between items-center mb-6">
              <div>
                <h3 className="text-2xl font-bold text-white">Edit Shop Item</h3>
                <p className="text-sm text-[var(--text-muted)] mt-1">Config Key: <span className="text-[var(--accent)] font-mono">{editingItemKey}</span></p>
              </div>
              <button 
                onClick={() => setEditingItemKey(null)}
                className="p-2 bg-[var(--bg-secondary)] rounded-full text-[var(--text-secondary)] hover:text-white transition-colors"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="md:col-span-2">
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-2">Display Name</label>
                <input 
                  type="text" 
                  value={form.name || ""} 
                  onChange={e => setForm({...form, name: e.target.value})} 
                  placeholder="e.g. §6Golden Shovel"
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-3 text-white focus:outline-none focus:border-[var(--accent)] transition-colors text-lg" 
                />
              </div>

              <div>
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-2">Material ID</label>
                <input 
                  type="text" 
                  value={form.material || ""} 
                  onChange={e => setForm({...form, material: e.target.value.toUpperCase()})} 
                  placeholder="e.g. GOLDEN_SHOVEL"
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-3 text-[var(--text-secondary)] focus:outline-none focus:border-[var(--accent)] font-mono" 
                />
              </div>

              <div>
                <label className="block text-xs uppercase tracking-widest font-bold text-[var(--text-muted)] mb-2">Quantity</label>
                <input 
                  type="number" 
                  value={form.amount || 1} 
                  onChange={e => setForm({...form, amount: Number(e.target.value)})} 
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-3 text-white focus:outline-none focus:border-[var(--accent)] font-bold" 
                />
              </div>
              
              <div className="md:col-span-2 pt-4 border-t border-[var(--border)] mt-2">
                <h4 className="text-sm font-bold text-white mb-4 flex items-center gap-2">
                  <svg className="w-4 h-4 text-emerald-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4 4a2 2 0 00-2 2v4a2 2 0 002 2V6h10a2 2 0 00-2-2H4zm2 6a2 2 0 012-2h8a2 2 0 012 2v4a2 2 0 01-2 2H8a2 2 0 01-2-2v-4zm6 4a2 2 0 100-4 2 2 0 000 4z" clipRule="evenodd" />
                  </svg>
                  Currency & Pricing
                </h4>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {form.price !== undefined && (
                    <div className="bg-emerald-500/5 p-4 rounded-2xl border border-emerald-500/10">
                      <label className="block text-xs font-bold text-emerald-400/70 mb-2 uppercase tracking-tighter">Standard Price (Money)</label>
                      <input type="number" value={form.price} onChange={e => setForm({...form, price: Number(e.target.value)})} className="w-full bg-transparent border-none p-0 text-xl text-emerald-400 font-black focus:outline-none" />
                    </div>
                  )}
                  {form["price-shards"] !== undefined && (
                    <div className="bg-emerald-500/5 p-4 rounded-2xl border border-emerald-500/10">
                      <label className="block text-xs font-bold text-emerald-400/70 mb-2 uppercase tracking-tighter">Kingdom Shards</label>
                      <input type="number" value={form["price-shards"]} onChange={e => setForm({...form, "price-shards": Number(e.target.value)})} className="w-full bg-transparent border-none p-0 text-xl text-emerald-400 font-black focus:outline-none" />
                    </div>
                  )}
                  {form["price-gems"] !== undefined && (
                    <div className="bg-cyan-500/5 p-4 rounded-2xl border border-cyan-500/10">
                      <label className="block text-xs font-bold text-cyan-400/70 mb-2 uppercase tracking-tighter">Web Gems (Premium)</label>
                      <input type="number" value={form["price-gems"]} onChange={e => setForm({...form, "price-gems": Number(e.target.value)})} className="w-full bg-transparent border-none p-0 text-xl text-cyan-400 font-black focus:outline-none" />
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="flex flex-col md:flex-row gap-3 justify-end mt-10">
              <button 
                onClick={() => setEditingItemKey(null)} 
                className="order-2 md:order-1 flex-1 md:flex-none px-8 py-3 rounded-xl text-sm font-bold bg-[var(--bg-secondary)] hover:bg-[var(--bg-card-hover)] text-[var(--text-secondary)] transition-all active:scale-95"
              >
                Discard Changes
              </button>
              <button 
                onClick={handleSaveItem} 
                className="order-1 md:order-2 flex-1 md:flex-none px-8 py-3 rounded-xl text-sm font-black bg-emerald-500 hover:bg-emerald-400 text-white shadow-lg shadow-emerald-500/20 transition-all active:scale-95 glow-emerald"
              >
                Update YAML Configuration
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
