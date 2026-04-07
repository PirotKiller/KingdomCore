import Link from "next/link";

export default function HomePage() {
  return (
    <div className="hero-gradient">
      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-24">
        <div className="text-center">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-[var(--border)] bg-[var(--bg-card)] text-sm text-[var(--text-secondary)] mb-8">
            <span className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
            Server Online
          </div>

          {/* Title */}
          <h1 className="text-5xl sm:text-7xl font-black tracking-tight mb-6">
            <span className="bg-gradient-to-r from-purple-400 via-purple-300 to-pink-400 bg-clip-text text-transparent">
              The Kingdom
            </span>
            <br />
            <span className="text-white">Web Store</span>
          </h1>

          <p className="text-lg sm:text-xl text-[var(--text-secondary)] max-w-2xl mx-auto mb-10 leading-relaxed">
            Power up your adventure. Buy Gems, Shards, exclusive items, and ranks to dominate the server.
          </p>

          {/* CTA */}
          <div className="flex items-center justify-center gap-4">
            <Link
              href="/store"
              className="px-8 py-3 text-lg font-bold rounded-xl bg-gradient-to-r from-[var(--accent)] to-purple-600 text-white hover:shadow-lg hover:shadow-purple-500/25 transition-all pulse-glow"
            >
              Browse Store
            </Link>
            <Link
              href="/account"
              className="px-8 py-3 text-lg font-semibold rounded-xl border border-[var(--border)] text-[var(--text-secondary)] hover:text-white hover:border-[var(--accent)] transition-all"
            >
              Link Account
            </Link>
          </div>
        </div>

        {/* Stats cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mt-20 max-w-3xl mx-auto">
          <div className="glass-card p-6 text-center float" style={{ animationDelay: "0s" }}>
            <div className="text-3xl font-bold text-amber-400">💎</div>
            <div className="text-sm text-[var(--text-secondary)] mt-2">Gems & Shards</div>
            <div className="text-xs text-[var(--text-muted)] mt-1">Premium Currency</div>
          </div>
          <div className="glass-card p-6 text-center float" style={{ animationDelay: "0.5s" }}>
            <div className="text-3xl font-bold text-emerald-400">⚔️</div>
            <div className="text-sm text-[var(--text-secondary)] mt-2">Exclusive Items</div>
            <div className="text-xs text-[var(--text-muted)] mt-1">Weapons & Armor</div>
          </div>
          <div className="glass-card p-6 text-center float" style={{ animationDelay: "1s" }}>
            <div className="text-3xl font-bold text-purple-400">👑</div>
            <div className="text-sm text-[var(--text-secondary)] mt-2">Ranks</div>
            <div className="text-xs text-[var(--text-muted)] mt-1">Unlock Perks</div>
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-24">
        <h2 className="text-3xl font-bold text-center mb-12">
          How It <span className="text-[var(--accent)]">Works</span>
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          {[
            { step: "1", icon: "🔐", title: "Login with Discord", desc: "Secure authentication" },
            { step: "2", icon: "🎮", title: "Link Minecraft", desc: "Verify in-game with /verify" },
            { step: "3", icon: "🛒", title: "Choose Items", desc: "Browse the store" },
            { step: "4", icon: "✨", title: "Instant Delivery", desc: "Items delivered on join" },
          ].map((s) => (
            <div key={s.step} className="glass-card p-6 text-center">
              <div className="text-2xl mb-3">{s.icon}</div>
              <div className="text-xs text-[var(--accent)] font-bold mb-1">STEP {s.step}</div>
              <div className="font-semibold text-white">{s.title}</div>
              <div className="text-sm text-[var(--text-muted)] mt-1">{s.desc}</div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
