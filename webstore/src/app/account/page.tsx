"use client";

import { useSession, signIn } from "next-auth/react";
import { useEffect, useState } from "react";
import Pagination from "@/components/Pagination";

export default function AccountPage() {
  const { data: session, status } = useSession();
  const [verifyState, setVerifyState] = useState<"idle" | "loading" | "code">("idle");
  const [code, setCode] = useState("");
  const [linkStatus, setLinkStatus] = useState<{ verified: boolean; minecraftUsername: string | null } | null>(null);
  const [purchases, setPurchases] = useState<any[]>([]);
  const [loadingPurchases, setLoadingPurchases] = useState(true);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  
  const [history, setHistory] = useState<any[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(true);
  const [historyPage, setHistoryPage] = useState(1);
  const [historyTotalPages, setHistoryTotalPages] = useState(1);
  const [historyTotal, setHistoryTotal] = useState(0);

  const loadPurchases = (p: number) => {
    setLoadingPurchases(true);
    fetch(`/api/account/purchases?page=${p}&limit=10`)
      .then((r) => r.json())
      .then((data) => {
        setPurchases(data.purchases || []);
        setTotalPages(data.totalPages || 1);
        setTotal(data.total || 0);
        setLoadingPurchases(false);
      })
      .catch(() => setLoadingPurchases(false));
  };

  const loadHistory = (p: number) => {
    setLoadingHistory(true);
    fetch(`/api/account/history?page=${p}&limit=5`)
      .then((r) => r.json())
      .then((data) => {
        setHistory(data.logs || []);
        setHistoryTotalPages(data.totalPages || 1);
        setHistoryTotal(data.total || 0);
        setLoadingHistory(false);
      })
      .catch(() => setLoadingHistory(false));
  };

  useEffect(() => {
    if (session) {
      fetch("/api/verify")
        .then((r) => r.json())
        .then(setLinkStatus)
        .catch(() => {});
      
      loadPurchases(page);
    }
  }, [session, page]);

  useEffect(() => {
    if (session && linkStatus?.verified) {
      loadHistory(historyPage);
    } else {
        setLoadingHistory(false);
    }
  }, [session, linkStatus?.verified, historyPage]);

  const generateCode = async () => {
    setVerifyState("loading");
    try {
      const res = await fetch("/api/verify", { method: "POST" });
      const data = await res.json();
      setCode(data.code);
      setVerifyState("code");
    } catch {
      setVerifyState("idle");
    }
  };

  if (status === "loading") {
    return (
      <div className="max-w-2xl mx-auto px-4 py-24">
        <div className="glass-card p-10 animate-pulse">
          <div className="h-8 w-48 bg-[var(--bg-secondary)] rounded mb-4" />
          <div className="h-4 w-full bg-[var(--bg-secondary)] rounded" />
        </div>
      </div>
    );
  }

  if (!session) {
    return (
      <div className="max-w-xl mx-auto px-4 py-24 text-center">
        <div className="glass-card p-10">
          <div className="text-5xl mb-6">🔒</div>
          <h1 className="text-2xl font-bold text-white mb-4">Sign In Required</h1>
          <p className="text-[var(--text-secondary)] mb-6">Login with Discord to manage your account.</p>
          <button
            onClick={() => signIn("discord")}
            className="px-6 py-3 rounded-xl bg-[#5865F2] hover:bg-[#4752C4] text-white font-semibold transition-colors"
          >
            Login with Discord
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-12">
      <h1 className="text-3xl font-bold mb-8">
        <span className="bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">My Account</span>
      </h1>

      {/* Discord Profile */}
      <div className="glass-card p-6 mb-6">
        <h2 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">Discord Account</h2>
        <div className="flex items-center gap-4">
          <img src={session.user?.image || ""} alt="" className="w-14 h-14 rounded-full border-2 border-[var(--accent)]" />
          <div>
            <div className="font-semibold text-white text-lg">{session.user?.name}</div>
            <div className="text-sm text-[var(--text-secondary)]">{session.user?.email || "Discord Connected"}</div>
          </div>
          <div className="ml-auto">
            <span className="px-3 py-1 text-xs font-semibold rounded-full bg-emerald-400/10 text-emerald-400 border border-emerald-400/20">
              Connected
            </span>
          </div>
        </div>
      </div>

      {/* Minecraft Link */}
      <div className="glass-card p-6 mb-6">
        <h2 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">Minecraft Account</h2>

        {linkStatus?.verified ? (
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-xl bg-[var(--bg-secondary)] border border-[var(--border)] overflow-hidden shadow-lg shadow-black/50">
              <img src={`https://mc-heads.net/avatar/${linkStatus.minecraftUuid || linkStatus.minecraftUsername}/64`} alt="" className="w-full h-full object-cover" />
            </div>
            <div>
              <div className="font-semibold text-white text-lg">{linkStatus.minecraftUsername}</div>
              <div className="text-sm text-emerald-400">✓ Verified & Linked</div>
            </div>
          </div>
        ) : (
          <div>
            <p className="text-[var(--text-secondary)] mb-4">
              Link your Minecraft account to receive purchases in-game.
            </p>

            {verifyState === "idle" && (
              <button
                onClick={generateCode}
                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-[var(--accent)] to-purple-600 text-white font-semibold text-sm transition-all hover:shadow-lg"
              >
                Generate Verification Code
              </button>
            )}

            {verifyState === "loading" && (
              <div className="text-[var(--text-secondary)]">Generating code...</div>
            )}

            {verifyState === "code" && (
              <div className="bg-[var(--bg-secondary)] rounded-xl p-6 border border-[var(--border)]">
                <p className="text-sm text-[var(--text-secondary)] mb-3">
                  Type this command in-game within 5 minutes:
                </p>
                <div className="bg-[var(--bg-primary)] rounded-lg p-4 text-center mb-3">
                  <code className="text-2xl font-mono font-bold text-[var(--accent)] tracking-widest">
                    /verify {code}
                  </code>
                </div>
                <p className="text-xs text-[var(--text-muted)]">
                  This code expires in 5 minutes. Refresh the page after verifying.
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Purchase History */}
      <div className="glass-card p-6">
        <h2 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">Recent Purchases</h2>
        
        {loadingPurchases ? (
          <div className="animate-pulse flex gap-4 mt-4">
            <div className="h-10 border border-[var(--border)] bg-[var(--bg-secondary)] rounded w-full"></div>
            <div className="h-10 bg-[var(--bg-secondary)] rounded w-full"></div>
          </div>
        ) : purchases.length > 0 ? (
          <>
            <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                  <th className="px-4 py-3 font-medium">Order ID</th>
                  <th className="px-4 py-3 font-medium">Item</th>
                  <th className="px-4 py-3 font-medium">Amount</th>
                  <th className="px-4 py-3 font-medium">Date</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {purchases.map((p) => (
                  <tr key={p._id} className="border-b border-[var(--border)] hover:bg-[var(--bg-card-hover)] transition-colors">
                    <td className="px-4 py-3">
                      <code className="text-[10px] font-bold text-blue-400 bg-blue-400/5 px-2 py-0.5 rounded border border-blue-400/20">
                        {p.transactionId || "LEGACY"}
                      </code>
                    </td>
                    <td className="px-4 py-3 font-medium text-white">{p.itemName}</td>
                    <td className="px-4 py-3 text-emerald-400">
                      {(p.price / 100).toLocaleString('en-US', { style: 'currency', currency: p.currency.toUpperCase() })}
                    </td>
                    <td className="px-4 py-3 text-[var(--text-secondary)]">
                      {new Date(p.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${p.status === "delivered" ? "bg-emerald-400/10 text-emerald-400" : "bg-amber-400/10 text-amber-400"}`}>
                        {p.status === "delivered" ? "Delivered" : "Pending"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={page}
            total={total}
            totalPages={totalPages}
            limit={10}
            onPageChange={setPage}
            loading={loadingPurchases}
          />
        </>
        ) : (
          <p className="text-[var(--text-secondary)] text-sm">Your purchase history will appear here after your first purchase.</p>
        )}
      </div>

      {/* Punishment History */}
      {linkStatus?.verified && (
        <div className="glass-card p-6 mt-6">
          <h2 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">Punishment History</h2>
          
          {loadingHistory ? (
            <div className="animate-pulse space-y-3">
              <div className="h-10 bg-[var(--bg-secondary)] rounded w-full"></div>
              <div className="h-10 bg-[var(--bg-secondary)] rounded w-full" style={{ opacity: 0.5 }}></div>
            </div>
          ) : history.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                    <th className="px-4 py-3 font-medium">Action</th>
                    <th className="px-4 py-3 font-medium">Reason</th>
                    <th className="px-4 py-3 font-medium">Duration</th>
                    <th className="px-4 py-3 font-medium text-right">Date</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((log) => (
                    <tr key={log._id} className="border-b border-[var(--border)] hover:bg-[var(--bg-card-hover)] transition-colors">
                      <td className="px-4 py-3">
                        <span className={`px-2 py-0.5 text-[10px] font-black uppercase tracking-widest rounded border ${
                            log.action === 'ban' || log.action === 'tempban' ? 'text-red-400 bg-red-400/10 border-red-400/20' :
                            log.action === 'kick' ? 'text-amber-400 bg-amber-400/10 border-amber-400/20' :
                            log.action === 'mute' ? 'text-violet-400 bg-violet-400/10 border-violet-400/20' :
                            'text-blue-400 bg-blue-400/10 border-blue-400/20'
                        }`}>
                          {log.action}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-[var(--text-secondary)] italic">
                        {log.reason || "No reason specified"}
                      </td>
                      <td className="px-4 py-3 text-[var(--text-muted)] font-mono text-xs">
                        {log.duration || "—"}
                      </td>
                      <td className="px-4 py-3 text-[var(--text-muted)] text-right tabular-nums">
                        {new Date(log.createdAt).toLocaleDateString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Pagination
                page={historyPage}
                total={historyTotal}
                totalPages={historyTotalPages}
                limit={5}
                onPageChange={setHistoryPage}
                loading={loadingHistory}
              />
            </div>
          ) : (
            <p className="text-[var(--text-secondary)] text-sm">Great job! You have no recorded punishments.</p>
          )}
        </div>
      )}
    </div>
  );
}
