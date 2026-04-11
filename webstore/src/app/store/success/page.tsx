"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useState, Suspense } from "react";

function SuccessContent() {
  const searchParams = useSearchParams();
  const sessionId = searchParams.get("session_id");
  const [order, setOrder] = useState<any>(null);
  const [loading, setLoading] = useState(!!sessionId);

  useEffect(() => {
    if (sessionId) {
      fetch(`/api/checkout/status?session_id=${sessionId}`)
        .then((r) => r.json())
        .then((data) => {
          if (!data.error) setOrder(data);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [sessionId]);

  return (
    <div className="max-w-xl mx-auto px-4 py-24 text-center">
      <div className="glass-card p-10">
        <div className="text-6xl mb-6">✅</div>
        <h1 className="text-3xl font-bold text-white mb-4">Purchase Successful!</h1>
        
        <p className="text-[var(--text-secondary)] mb-4">
          Your order has been processed successfully.
        </p>

        {loading ? (
          <div className="mb-8 p-4 bg-white/5 rounded-xl border border-white/10 animate-pulse">
            <div className="h-3 w-24 bg-white/10 mx-auto rounded mb-2" />
            <div className="h-5 w-32 bg-white/20 mx-auto rounded" />
          </div>
        ) : order ? (
          <div className="mb-8 p-6 bg-emerald-400/5 rounded-2xl border border-emerald-400/20 relative overflow-hidden group">
            {/* Subtle glow effect */}
            <div className="absolute inset-0 bg-gradient-to-br from-emerald-400/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
            
            <div className="relative z-10">
              <div className="text-[10px] font-bold text-emerald-400 uppercase tracking-widest mb-2">Order Tracking ID</div>
              <div className="text-2xl font-mono font-black text-white selection:bg-emerald-400 selection:text-black tracking-wider">
                {order.transactionId}
              </div>
              <div className="mt-3 text-xs text-[var(--text-muted)] italic">
                Save this ID for your records
              </div>
            </div>
          </div>
        ) : null}

        <div className="space-y-4 mb-8">
            <p className="text-[var(--text-secondary)] text-sm">
            Currency purchases (Gems/Shards) are delivered instantly.
            </p>
            <p className="text-[var(--text-secondary)] text-sm">
            Other items will be delivered the next time you join the server.
            </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
            href="/store"
            className="px-6 py-3 rounded-xl bg-white/5 border border-white/10 text-white font-semibold hover:bg-white/10 transition-all"
            >
            Back to Store
            </Link>
            <Link
            href="/account"
            className="px-6 py-3 rounded-xl bg-gradient-to-r from-[var(--accent)] to-purple-600 text-white font-semibold hover:shadow-lg transition-all"
            >
            View My Account
            </Link>
        </div>
      </div>
    </div>
  );
}

export default function SuccessPage() {
  return (
    <Suspense fallback={
        <div className="max-w-xl mx-auto px-4 py-24 text-center">
            <div className="glass-card p-10 animate-pulse">
                <div className="h-16 w-16 bg-white/5 rounded-full mx-auto mb-6" />
                <div className="h-8 w-48 bg-white/5 mx-auto rounded mb-4" />
                <div className="h-4 w-full bg-white/5 mx-auto rounded" />
            </div>
        </div>
    }>
      <SuccessContent />
    </Suspense>
  );
}
