"use client";

import { useSession } from "next-auth/react";
import { useState } from "react";

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
    if (!session) return;
    setLoading(true);
    try {
      const res = await fetch("/api/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ itemId: item._id }),
      });
      const data = await res.json();
      if (data.url) {
        window.location.href = data.url;
      } else {
        alert(data.error || "Something went wrong");
      }
    } catch (err) {
      alert("Failed to start checkout");
    } finally {
      setLoading(false);
    }
  };

  const priceDisplay = `$${(item.price / 100).toFixed(2)}`;
  const categoryColors: Record<string, string> = {
    currency: "text-amber-400 bg-amber-400/10 border-amber-400/20",
    items: "text-emerald-400 bg-emerald-400/10 border-emerald-400/20",
    ranks: "text-purple-400 bg-purple-400/10 border-purple-400/20",
  };

  return (
    <div className={`glass-card p-5 flex flex-col gap-3 hover:border-[var(--accent)] transition-all duration-300 group ${item.featured ? "glow-accent" : ""}`}>
      {/* Category badge */}
      <div className="flex items-center justify-between">
        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${categoryColors[item.category] || ""}`}>
          {item.category.toUpperCase()}
        </span>
        {item.featured && (
          <span className="text-xs text-amber-400">⭐ Featured</span>
        )}
      </div>

      {/* Icon/Image area */}
      <div className="w-full h-40 bg-[var(--bg-secondary)] rounded-xl relative overflow-hidden group-hover:shadow-[0_0_20px_rgba(var(--accent-rgb),0.3)] transition-all duration-500">
        {item.imageUrl ? (
          <>
            {/* Base Image */}
            <img 
              src={item.imageUrl} 
              alt={item.name} 
              className="absolute inset-0 w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-out"
            />
            {/* Gradient Overlay for blending */}
            <div className="absolute inset-0 bg-gradient-to-t from-[var(--bg-card)] via-transparent to-transparent opacity-80" />
          </>
        ) : (
          <div className="absolute inset-0 flex items-center justify-center text-6xl group-hover:scale-110 transition-transform duration-500 ease-out bg-gradient-to-br from-[var(--bg-secondary)] to-[var(--bg-card)]">
            {item.category === "currency" ? "💎" : item.category === "ranks" ? "👑" : "⚔️"}
          </div>
        )}
      </div>

      {/* Name & description */}
      <h3 className="text-lg font-bold text-white">{item.name}</h3>
      <p className="text-sm text-[var(--text-secondary)] leading-relaxed flex-1">{item.description}</p>

      {/* Price & buy */}
      <div className="flex items-center justify-between mt-2">
        <span className="text-xl font-bold bg-gradient-to-r from-emerald-400 to-emerald-300 bg-clip-text text-transparent">
          {priceDisplay}
        </span>
        <button
          onClick={handleBuy}
          disabled={loading || !session}
          className="px-4 py-2 text-sm font-semibold rounded-lg bg-gradient-to-r from-[var(--accent)] to-purple-600 hover:from-purple-600 hover:to-[var(--accent)] text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? "..." : session ? "Buy Now" : "Login to Buy"}
        </button>
      </div>
    </div>
  );
}
