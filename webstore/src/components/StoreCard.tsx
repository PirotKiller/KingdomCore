"use client";

import { useSession, signIn } from "next-auth/react";
import { useState } from "react";
import toast from "react-hot-toast";

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

export default function StoreCard({ item }: { item: StoreItemData }) {
  const { data: session } = useSession();
  const [loading, setLoading] = useState(false);

  const handleBuy = async () => {
    if (!session) {
      signIn("discord");
      return;
    }
    
    setLoading(true);
    const loadingToast = toast.loading("Preparing checkout...");
    try {
      const res = await fetch("/api/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ itemId: item._id }),
      });
      const data = await res.json();
      if (data.url) {
        toast.success("Redirecting to checkout...", { id: loadingToast });
        window.location.href = data.url;
      } else {
        toast.error(data.error || "Something went wrong", { id: loadingToast });
      }
    } catch (err) {
      toast.error("Failed to start checkout", { id: loadingToast });
    } finally {
      setLoading(false);
    }
  };

  const priceDisplay = `$${(item.price / 100).toFixed(2)}`;
  
  const categoryConfig: Record<string, { colors: string; icon: string }> = {
    currency: { colors: "text-amber-400 bg-amber-400/10 border-amber-400/20", icon: "💎" },
    items: { colors: "text-emerald-400 bg-emerald-400/10 border-emerald-400/20", icon: "⚔️" },
    ranks: { colors: "text-purple-400 bg-purple-400/10 border-purple-400/20", icon: "👑" },
  };

  const config = categoryConfig[item.category] || { colors: "text-gray-400 bg-gray-400/10", icon: "📦" };
  const isPremium = item.featured;

  return (
    <div className={`relative group transition-all duration-500 ${isPremium ? "premium-card-container scale-[1.02] shadow-[0_20px_50px_rgba(139,92,246,0.2)]" : "hover:-translate-y-2"}`}>
      {isPremium && <div className="premium-card-bg" />}
      
      <div className={`${isPremium ? "premium-card-inner" : "glass-card hover:bg-white/[0.03] group-hover:border-white/20 transition-all duration-500"} p-6 flex flex-col gap-4 relative h-full`}>
        {isPremium && <div className="premium-pulse" />}
        
        {/* Category badge */}
        <div className="flex items-center justify-between relative z-20">
          <span className={`text-[10px] font-black tracking-[0.2em] px-3 py-1 rounded-lg border flex items-center gap-2 ${config.colors}`}>
            <span>{config.icon}</span>
            {item.category.toUpperCase()}
          </span>
          {isPremium && (
            <span className="text-[10px] font-black text-white bg-gradient-to-r from-amber-400 to-orange-500 px-3 py-1 rounded-full shadow-[0_0_15px_rgba(245,158,11,0.5)] uppercase tracking-widest animate-pulse">
              ★ Exclusive
            </span>
          )}
        </div>

        {/* Icon/Image area */}
        <div className="w-full h-48 bg-white/5 rounded-2xl relative overflow-hidden group-hover:shadow-[0_0_30px_rgba(139,92,246,0.3)] transition-all duration-700 z-10">
          {item.imageUrl ? (
            <>
              <img 
                src={item.imageUrl} 
                alt={item.name} 
                className="absolute inset-0 w-full h-full object-cover group-hover:scale-110 transition-transform duration-1000 ease-out"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#12121a] via-transparent to-transparent opacity-60" />
            </>
          ) : (
            <div className="absolute inset-0 flex items-center justify-center text-7xl group-hover:scale-125 transition-transform duration-700 ease-out bg-gradient-to-br from-white/5 to-transparent">
              {config.icon}
            </div>
          )}
          {isPremium && <div className="absolute inset-0 shine-sweep opacity-30" />}
        </div>

        {/* Name & description */}
        <div className="space-y-2 flex-1 relative z-10">
          <h3 className={`text-xl font-black tracking-tight group-hover:text-[var(--accent)] transition-colors duration-300 ${isPremium ? "text-white" : "text-white"}`}>
            {item.name}
          </h3>
          <p className="text-sm text-[var(--text-secondary)] leading-relaxed line-clamp-3 font-medium">
            {item.description}
          </p>
        </div>

        {/* Price & buy */}
        <div className="flex items-center justify-between mt-4 pt-4 border-t border-white/5 relative z-10">
          <div className="flex flex-col">
            <span className="text-[9px] font-black text-[var(--text-muted)] uppercase tracking-[0.2em]">Investment</span>
            <span className={`text-2xl font-black ${isPremium ? "text-transparent bg-clip-text bg-gradient-to-r from-white to-purple-300" : "text-white"}`}>
              {priceDisplay}
            </span>
          </div>
          
          <button
            onClick={handleBuy}
            disabled={loading}
            className={`relative overflow-hidden px-7 py-3.5 text-xs font-black uppercase tracking-widest rounded-xl transition-all hover:pr-12 disabled:opacity-50 disabled:cursor-not-allowed group/btn ${
              isPremium 
                ? "bg-gradient-to-r from-[var(--accent)] to-purple-600 text-white shadow-lg shadow-purple-500/30" 
                : "bg-white text-black"
            }`}
          >
            <span className="relative z-10">{loading ? "Wait..." : session ? "Acquire" : "Login"}</span>
            <span className="absolute right-4 top-1/2 -translate-y-1/2 opacity-0 group-hover/btn:opacity-100 group-hover/btn:translate-x-0 -translate-x-2 transition-all duration-300">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}
