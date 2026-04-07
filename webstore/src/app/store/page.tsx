"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import StoreCard from "@/components/StoreCard";

interface StoreItemData {
  _id: string;
  name: string;
  description: string;
  category: string;
  price: number;
  currency: string;
  imageUrl?: string;
  deliveryType: string;
  featured: boolean;
}

const categories = [
  { key: "all", label: "All Items", icon: "🏪" },
  { key: "currency", label: "Currency", icon: "💎" },
  { key: "items", label: "Items", icon: "⚔️" },
  { key: "ranks", label: "Ranks", icon: "👑" },
];

export default function StorePage() {
  const [items, setItems] = useState<StoreItemData[]>([]);
  const [activeCategory, setActiveCategory] = useState("all");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const params = activeCategory === "all" ? "" : `?category=${activeCategory}`;
    fetch(`/api/store${params}`)
      .then((r) => r.json())
      .then((data) => { setItems(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, [activeCategory]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Back Button */}
      <div className="mb-6">
        <Link href="/" className="inline-flex items-center gap-2 text-sm font-medium text-[var(--text-secondary)] hover:text-white bg-[var(--bg-card)] hover:bg-[var(--bg-card-hover)] border border-[var(--border)] px-4 py-2 rounded-xl transition-all shadow-sm">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
          Back to Home
        </Link>
      </div>

      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold mb-3">
          <span className="bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">Store</span>
        </h1>
        <p className="text-[var(--text-secondary)]">Choose your upgrades and dominate the server</p>
      </div>

      {/* Category Tabs */}
      <div className="flex items-center justify-center mb-10 px-4">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-2 max-w-full">
          {categories.map((cat) => (
            <button
              key={cat.key}
              onClick={() => { setActiveCategory(cat.key); setLoading(true); }}
              className={`px-4 py-2 rounded-xl text-sm font-medium transition-all border shrink-0 ${
                activeCategory === cat.key
                  ? "bg-[var(--accent)] border-[var(--accent)] text-white"
                  : "bg-[var(--bg-card)] border-[var(--border)] text-[var(--text-secondary)] hover:border-[var(--accent)] hover:text-white"
              }`}
            >
              {cat.icon} {cat.label}
            </button>
          ))}
        </div>
      </div>

      {/* Items Grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="glass-card p-5 h-72 animate-pulse">
              <div className="h-4 w-20 bg-[var(--bg-secondary)] rounded mb-4" />
              <div className="h-24 bg-[var(--bg-secondary)] rounded-xl mb-4" />
              <div className="h-4 w-32 bg-[var(--bg-secondary)] rounded mb-2" />
              <div className="h-3 w-full bg-[var(--bg-secondary)] rounded" />
            </div>
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-20">
          <div className="text-5xl mb-4">🏪</div>
          <h3 className="text-xl font-semibold text-[var(--text-secondary)]">No items available</h3>
          <p className="text-[var(--text-muted)] mt-2">Check back soon for new additions!</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {items.map((item) => (
            <StoreCard key={item._id} item={item} />
          ))}
        </div>
      )}
    </div>
  );
}
