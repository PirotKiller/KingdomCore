"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import StoreCard from "@/components/StoreCard";
import { useSession, signIn } from "next-auth/react";

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
  const [searchQuery, setSearchQuery] = useState("");
  const [sortOption, setSortOption] = useState("default");
  const { data: session, status } = useSession();

  useEffect(() => {
    const params = activeCategory === "all" ? "" : `?category=${activeCategory}`;
    fetch(`/api/store${params}`)
      .then((r) => r.json())
      .then((data) => { setItems(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, [activeCategory]);

  return (
    <div className="min-h-screen mesh-gradient relative overflow-hidden">
      {/* Background Decorations */}
      <div className="absolute top-20 left-10 w-72 h-72 bg-purple-600 rounded-full float-orb" />
      <div className="absolute bottom-20 right-10 w-96 h-96 bg-emerald-600 rounded-full float-orb" style={{ animationDelay: "-3s" }} />
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-full grid-pattern opacity-[0.03] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 relative z-10">
        {/* Back Button */}
        <div className="mb-10">
          <Link href="/" className="inline-flex items-center gap-2 text-sm font-semibold text-[var(--text-secondary)] hover:text-white bg-white/5 hover:bg-white/10 border border-white/10 px-5 py-2.5 rounded-2xl transition-all backdrop-blur-md group">
            <svg className="w-4 h-4 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Back to Home
          </Link>
        </div>

        {/* Login Banner for Guests */}
        {!session && status !== "loading" && (
          <div className="mb-10 p-4 border border-amber-500/20 bg-amber-500/10 rounded-2xl flex items-center justify-between glass-card animate-fade-in">
            <div className="flex items-center gap-3">
              <span className="text-2xl">👋</span>
              <div>
                <h3 className="text-white font-bold">Welcome Traveler!</h3>
                <p className="text-sm text-white/70">Connect your Discord account to purchase ranks and items.</p>
              </div>
            </div>
            <button 
              onClick={() => signIn("discord")}
              className="px-5 py-2 bg-indigo-500 text-white rounded-xl font-bold hover:bg-indigo-600 transition-colors"
            >
              Sign In
            </button>
          </div>
        )}

        {/* Hero Section */}
        <div className="text-center mb-16 space-y-4">
          <div className="inline-block px-4 py-1.5 rounded-full bg-white/5 border border-white/10 text-[10px] font-bold uppercase tracking-[0.2em] text-[var(--accent)] mb-2 animate-pulse">
            Premium Server Assets
          </div>
          <h1 className="text-5xl md:text-7xl font-black tracking-tight text-white">
            THE <span className="text-transparent bg-clip-text bg-gradient-to-r from-[var(--accent)] to-pink-500">KINGDOM</span> SHOP
          </h1>
          <p className="max-w-2xl mx-auto text-lg text-[var(--text-secondary)] font-medium">
            Equip yourself with the finest ranks, currencies, and items. 
            All purchases directly support the growth of the realm.
          </p>
        </div>

        {/* Enhanced Category Tabs */}
        <div className="flex items-center justify-center mb-16 px-4">
          <div className="flex items-center gap-3 p-2 bg-white/5 border border-white/10 rounded-[24px] backdrop-blur-xl overflow-x-auto no-scrollbar max-w-full">
            {categories.map((cat) => (
              <button
                key={cat.key}
                onClick={() => { setActiveCategory(cat.key); setLoading(true); }}
                className={`px-6 py-3 rounded-[18px] text-sm font-bold transition-all shrink-0 flex items-center gap-2 ${
                  activeCategory === cat.key
                    ? "bg-gradient-to-r from-[var(--accent)] to-purple-600 text-white shadow-[0_8px_20px_rgba(139,92,246,0.3)] scale-105"
                    : "text-[var(--text-secondary)] hover:text-white hover:bg-white/5"
                }`}
              >
                <span className="text-lg">{cat.icon}</span>
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        {/* Search and Sort Toolbar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 mb-8">
          <div className="relative w-full sm:w-96">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-white/40">🔍</span>
            <input 
              type="text" 
              placeholder="Search items..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-white/5 border border-white/10 px-12 py-3 rounded-xl text-white focus:outline-none focus:border-[var(--accent)] transition-colors"
            />
          </div>
          <select 
            value={sortOption}
            onChange={(e) => setSortOption(e.target.value)}
            className="w-full sm:w-48 bg-white/5 border border-white/10 px-4 py-3 rounded-xl text-white focus:outline-none focus:border-[var(--accent)] transition-colors appearance-none"
          >
            <option value="default" className="bg-[#1a1b23]">Sort Custom</option>
            <option value="price-asc" className="bg-[#1a1b23]">Price: Low to High</option>
            <option value="price-desc" className="bg-[#1a1b23]">Price: High to Low</option>
            <option value="name-asc" className="bg-[#1a1b23]">Name: A-Z</option>
          </select>
        </div>

        {/* Items Grid */}
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="glass-card p-6 h-80 animate-pulse bg-white/5 border-white/10">
                <div className="h-4 w-20 bg-white/5 rounded mb-4" />
                <div className="h-32 bg-white/5 rounded-2xl mb-4" />
                <div className="h-5 w-32 bg-white/5 rounded mb-2" />
                <div className="h-3 w-full bg-white/5 rounded" />
              </div>
            ))}
          </div>
        ) : items.length === 0 ? (
          <div className="text-center py-24 glass-card bg-white/5 border-white/10 max-w-lg mx-auto">
            <div className="text-6xl mb-6 float-orb opacity-100">🏪</div>
            <h3 className="text-2xl font-bold text-white">No items available</h3>
            <p className="text-[var(--text-secondary)] mt-3">Check back soon for new additions!</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 pb-20">
            {[...items]
              .filter(item => item.name.toLowerCase().includes(searchQuery.toLowerCase()))
              .sort((a, b) => {
                if (sortOption === "price-asc") return a.price - b.price;
                if (sortOption === "price-desc") return b.price - a.price;
                if (sortOption === "name-asc") return a.name.localeCompare(b.name);
                return 0;
              })
              .map((item) => (
              <StoreCard key={item._id} item={item} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
