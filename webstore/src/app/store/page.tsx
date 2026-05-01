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

const sortOptions = [
  { key: "default", label: "Sort: Featured" },
  { key: "price-asc", label: "Price: Low-High" },
  { key: "price-desc", label: "Price: High-Low" },
  { key: "name-asc", label: "Name: A-Z" },
];

export default function StorePage() {
  const [items, setItems] = useState<StoreItemData[]>([]);
  const [activeCategory, setActiveCategory] = useState("all");
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortOption, setSortOption] = useState("default");
  const [dropdownOpen, setDropdownOpen] = useState(false);
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
          <div className="mb-12 p-6 rounded-3xl flex flex-col md:flex-row items-center justify-between gap-6 relative overflow-hidden group glass-card border border-white/10 shadow-[0_20px_50px_rgba(88,101,242,0.1)]">
            <div className="absolute inset-0 bg-gradient-to-r from-[#5865F2]/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-700" />
            
            <div className="flex flex-col sm:flex-row items-center gap-5 relative z-10 text-center sm:text-left">
              <div className="w-16 h-16 rounded-2xl bg-white/5 flex items-center justify-center text-3xl border border-white/10 shadow-inner group-hover:scale-110 transition-transform duration-500">
                👋
              </div>
              <div className="space-y-1">
                <h3 className="text-xl font-black text-white tracking-tight uppercase">Welcome Traveler!</h3>
                <p className="text-sm text-[var(--text-secondary)] font-medium max-w-sm">
                  Join our discord community to unlock access to premium ranks and exclusive server items.
                </p>
              </div>
            </div>

            <button 
              onClick={() => signIn("discord")}
              className="w-full md:w-auto px-8 py-4 bg-[#5865F2] text-white rounded-2xl font-black uppercase text-xs tracking-widest hover:bg-[#4752C4] transition-all hover:scale-[1.02] shadow-xl shadow-indigo-500/20 active:scale-95 relative z-10"
            >
              Connect Discord
            </button>
          </div>
        )}

        {/* Hero Section */}
        <div className="text-center mb-10 space-y-4">
          <div className="inline-block px-4 py-1.5 rounded-full bg-white/5 border border-white/10 text-[10px] font-bold uppercase tracking-[0.2em] text-[var(--accent)] mb-2 animate-pulse">
            Premium Server Assets
          </div>
          <h1 className="text-4xl sm:text-5xl md:text-7xl font-black tracking-tight text-white leading-tight">
            THE <span className="text-transparent bg-clip-text bg-gradient-to-r from-[var(--accent)] to-pink-500">KINGDOM</span> SHOP
          </h1>
        </div>

        {/* Categories Grid - All Visible */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4 mb-12">
          {categories.map((cat) => (
            <button
              key={cat.key}
              onClick={() => { setActiveCategory(cat.key); setLoading(true); }}
              className={`relative overflow-hidden group p-4 sm:p-6 rounded-[24px] transition-all duration-500 border ${
                activeCategory === cat.key
                  ? "bg-[var(--accent)]/10 border-[var(--accent)] ring-1 ring-[var(--accent)]/50"
                  : "bg-white/5 border-white/10 hover:border-white/20 hover:bg-white/[0.08]"
              }`}
            >
              <div className="relative z-10 flex flex-col items-center gap-3">
                <span className={`text-3xl sm:text-4xl transition-transform duration-500 group-hover:scale-110 ${activeCategory === cat.key ? "scale-110" : ""}`}>
                  {cat.icon}
                </span>
                <span className={`text-xs sm:text-sm font-black tracking-widest uppercase ${activeCategory === cat.key ? "text-white" : "text-[var(--text-secondary)]"}`}>
                  {cat.label}
                </span>
              </div>
              
              {/* Background Glow for active state */}
              {activeCategory === cat.key && (
                <div className="absolute inset-0 bg-gradient-to-br from-[var(--accent)]/20 to-transparent animate-pulse" />
              )}
            </button>
          ))}
        </div>

        {/* Search and Sort Toolbar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-3 bg-white/5 border border-white/10 rounded-[28px] backdrop-blur-2xl mb-8 relative z-30">
          <div className="relative w-full sm:w-96">
            <span className="absolute left-5 top-1/2 -translate-y-1/2 text-white/40">🔍</span>
            <input 
              type="text" 
              placeholder="Search products..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-transparent border-none px-12 py-3 rounded-xl text-white focus:outline-none placeholder:text-white/20 text-sm font-medium"
            />
          </div>
          
          <div className="flex items-center gap-4 w-full sm:w-auto px-2 relative">
            <div className="h-8 w-px bg-white/10 hidden sm:block" />
            
            {/* Custom Premium Dropdown */}
            <div className="relative w-full sm:w-56">
              <button
                onClick={() => setDropdownOpen(!dropdownOpen)}
                className="w-full flex items-center justify-between bg-white/5 border border-white/10 px-5 py-2.5 rounded-2xl text-white hover:bg-white/10 transition-all group"
              >
                <div className="flex flex-col items-start">
                  <span className="text-[9px] font-black text-[var(--text-muted)] uppercase tracking-widest leading-none mb-1">Sort by</span>
                  <span className="text-[11px] font-black uppercase tracking-wider">
                    {sortOptions.find(o => o.key === sortOption)?.label.split(": ")[1] || "Featured"}
                  </span>
                </div>
                <svg 
                  className={`w-4 h-4 text-[var(--text-muted)] transition-transform duration-300 ${dropdownOpen ? "rotate-180" : ""}`} 
                  fill="none" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {dropdownOpen && (
                <>
                  <div 
                    className="fixed inset-0 z-40" 
                    onClick={() => setDropdownOpen(false)} 
                  />
                  <div className="absolute right-0 top-full mt-2 w-full min-w-[200px] bg-[#1a1b26] border border-white/20 rounded-2xl shadow-[0_20px_50px_rgba(0,0,0,0.5)] z-50 overflow-hidden animate-fade-in backdrop-blur-xl">
                    {sortOptions.map((option) => (
                      <button
                        key={option.key}
                        onClick={() => {
                          setSortOption(option.key);
                          setDropdownOpen(false);
                        }}
                        className={`w-full flex items-center justify-between px-5 py-4 text-left transition-all hover:bg-white/10 ${
                          sortOption === option.key ? "bg-white/[0.12] text-[var(--accent)]" : "text-white/80 hover:text-white"
                        }`}
                      >
                        <span className="text-xs font-black uppercase tracking-widest">{option.label.split(": ")[1]}</span>
                        {sortOption === option.key && (
                          <div className="w-2 h-2 rounded-full bg-[var(--accent)] shadow-[0_0_12px_var(--accent)]" />
                        )}
                      </button>
                    ))}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Items Grid */}
        <div className="max-w-full">
            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
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
              <div className="text-center py-24 glass-card bg-white/5 border-white/10 max-w-lg mx-auto fade-in">
                <div className="text-6xl mb-6 float-orb opacity-100">🏪</div>
                <h3 className="text-2xl font-bold text-white">No items available</h3>
                <p className="text-[var(--text-secondary)] mt-3">Check back soon for new additions!</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6 pb-20 fade-in">
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
    </div>
  );
}
