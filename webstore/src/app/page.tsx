"use client";

import Link from "next/link";
import useSWR from "swr";

const fetcher = (url: string) => fetch(url).then((res) => res.json());

export default function HomePage() {
  const { data: serverStatus } = useSWR("/api/server/status", fetcher, {
    refreshInterval: 60000, // Update every minute
  });

  return (
    <div className="hero-gradient">
      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-24 relative overflow-hidden">
        {/* Background Decorative Mesh Gradients */}
        <div className="absolute top-0 left-1/4 w-[500px] h-[500px] bg-purple-600/20 rounded-full blur-[120px] -z-10 animate-pulse" />
        <div className="absolute bottom-0 right-1/4 w-[400px] h-[400px] bg-emerald-600/10 rounded-full blur-[100px] -z-10" />

        <div className="text-center relative">
          {/* Real-time Status Badge */}
          <div className="inline-flex items-center gap-3 px-4 py-2 rounded-2xl border border-white/10 bg-white/5 backdrop-blur-md text-sm font-semibold mb-10 transition-all hover:border-white/20">
            {serverStatus?.online ? (
              <>
                <span className="flex h-2.5 w-2.5 relative">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
                </span>
                <span className="text-white">Server Online</span>
                <span className="w-1 h-1 bg-white/30 rounded-full" />
                <span className="text-emerald-400">{serverStatus.players.online} Players Online</span>
              </>
            ) : serverStatus?.online === false ? (
              <>
                <span className="w-2.5 h-2.5 bg-red-500 rounded-full" />
                <span className="text-white">Server Offline</span>
                <span className="text-red-400/50">Maintenance</span>
              </>
            ) : (
              // Loading State
              <>
                <span className="w-2.5 h-2.5 bg-white/20 rounded-full animate-pulse" />
                <span className="text-[var(--text-secondary)]">Checking Status...</span>
              </>
            )}
          </div>

          {/* Title with Advanced Gradient Styling */}
          <h1 className="text-5xl sm:text-7xl lg:text-8xl font-black tracking-tighter mb-6 sm:mb-8 leading-tight sm:leading-none">
            <span className="bg-gradient-to-br from-white via-purple-300 to-purple-500 bg-clip-text text-transparent drop-shadow-sm">
              THE KINGDOM
            </span>
            <br />
            <span className="text-white">WEB STORE</span>
          </h1>

          <p className="text-lg sm:text-xl lg:text-2xl text-[var(--text-secondary)] max-w-2xl mx-auto mb-10 sm:mb-12 leading-relaxed font-medium px-4">
            Power up your adventure. Buy Gems, Shards, exclusive items, and ranks to dominate the server.
          </p>

          {/* CTA with Glassmorphism and Hover Effects */}
          <div className="flex flex-col xs:flex-row items-center justify-center gap-4 sm:gap-6 px-4">
            <Link
              href="/store"
              className="w-full xs:w-auto group relative px-8 sm:px-10 py-4 text-lg sm:text-xl font-bold rounded-2xl bg-white text-black hover:bg-[var(--accent)] hover:text-white transition-all overflow-hidden text-center"
            >
              <span className="relative z-10 flex items-center justify-center gap-2">
                Browse Store
                <svg className="w-5 h-5 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
              </span>
            </Link>
            <Link
              href="/account"
              className="w-full xs:w-auto px-8 sm:px-10 py-4 text-lg sm:text-xl font-bold rounded-2xl border-2 border-white/10 text-white hover:bg-white/5 hover:border-white/20 transition-all backdrop-blur-sm text-center"
            >
              Link Account
            </Link>
          </div>
        </div>

        {/* Stats cards with premium layout */}
        <div className="grid grid-cols-1 xs:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-8 mt-16 sm:mt-24 max-w-5xl mx-auto px-4">
          {[
            { icon: "💎", title: "Gems & Shards", sub: "Premium Currency", color: "amber" },
            { icon: "⚔️", title: "Exclusive Items", sub: "Weapons & Armor", color: "emerald" },
            { icon: "👑", title: "Ranks", sub: "Unlock Perks", color: "purple" },
          ].map((stat, i) => (
            <div 
              key={stat.title} 
              className={`glass-card p-6 sm:p-10 text-center float group hover:bg-white/[0.03] transition-colors ${i === 2 ? 'xs:col-span-2 lg:col-span-1' : ''}`}
              style={{ animationDelay: `${i * 0.2}s` }}
            >
              <div className="text-4xl sm:text-5xl mb-4 sm:mb-6 group-hover:scale-110 transition-transform duration-500">{stat.icon}</div>
              <div className="text-base sm:text-lg font-bold text-white">{stat.title}</div>
              <div className="text-xs sm:text-sm text-[var(--text-secondary)] mt-1">{stat.sub}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Modernized How It Works Section */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 pb-32">
        <div className="flex flex-col items-center mb-16">
          <span className="text-[var(--accent)] font-black tracking-widest text-xs uppercase mb-4">Process</span>
          <h2 className="text-4xl sm:text-5xl font-black text-white text-center">
            HOW IT <span className="underline decoration-[var(--accent)] decoration-8 underline-offset-8">WORKS</span>
          </h2>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {[
            { step: "1", icon: "🔐", title: "Discord", desc: "Secure authentication login" },
            { step: "2", icon: "🎮", title: "Minecraft", desc: "Verify in-game with /verify" },
            { step: "3", icon: "🛒", title: "Choose", desc: "Select your premium items" },
            { step: "4", icon: "✨", title: "Deliver", desc: "Instant delivery on join" },
          ].map((s) => (
            <div key={s.step} className="glass-card p-8 text-center border-white/5 hover:border-white/20 transition-all group">
              <div className="w-16 h-16 rounded-2xl bg-white/5 flex items-center justify-center text-3xl mx-auto mb-6 group-hover:bg-[var(--accent)]/20 transition-colors">
                {s.icon}
              </div>
              <div className="text-[10px] text-[var(--accent)] font-black mb-2 uppercase tracking-tighter">Phase {s.step}</div>
              <div className="text-xl font-bold text-white mb-2">{s.title}</div>
              <div className="text-sm text-[var(--text-muted)] leading-relaxed">{s.desc}</div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
