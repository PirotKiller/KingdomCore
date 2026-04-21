"use client";

import { useState, useEffect } from "react";
import Pagination from "@/components/Pagination";

interface ModerationLog {
  _id: string;
  action: string;
  playerName: string | null;
  reason: string | null;
  duration: string | null;
  label: string;
  adminName: string;
  createdAt: string;
}

const ACTIONS = [
  { id: "kick", label: "Kick", icon: "🦶", color: "amber", requiresPlayer: true, hasReason: true, hasDuration: false, isCustom: false },
  { id: "ban", label: "Ban", icon: "🔨", color: "red", requiresPlayer: true, hasReason: true, hasDuration: false, isCustom: false },
  { id: "tempban", label: "Temp Ban", icon: "⏱️", color: "orange", requiresPlayer: true, hasReason: true, hasDuration: true, isCustom: false },
  { id: "unban", label: "Unban", icon: "🔓", color: "emerald", requiresPlayer: true, hasReason: false, hasDuration: false, isCustom: false },
  { id: "mute", label: "Mute", icon: "🔇", color: "violet", requiresPlayer: true, hasReason: false, hasDuration: true, isCustom: false },
  { id: "unmute", label: "Unmute", icon: "🔊", color: "cyan", requiresPlayer: true, hasReason: false, hasDuration: false, isCustom: false },
  { id: "warn", label: "Warn", icon: "⚠️", color: "yellow", requiresPlayer: true, hasReason: true, hasDuration: false, isCustom: false },
  { id: "custom", label: "Custom Command", icon: "⌨️", color: "blue", requiresPlayer: false, hasReason: false, hasDuration: false, isCustom: true },
] as const;

const actionColorMap: Record<string, string> = {
  kick: "text-amber-400 bg-amber-400/10 border-amber-400/20",
  ban: "text-red-400 bg-red-400/10 border-red-400/20",
  tempban: "text-orange-400 bg-orange-400/10 border-orange-400/20",
  unban: "text-emerald-400 bg-emerald-400/10 border-emerald-400/20",
  mute: "text-violet-400 bg-violet-400/10 border-violet-400/20",
  unmute: "text-cyan-400 bg-cyan-400/10 border-cyan-400/20",
  warn: "text-yellow-400 bg-yellow-400/10 border-yellow-400/20",
  custom: "text-blue-400 bg-blue-400/10 border-blue-400/20",
};

export default function AdminModerationPage() {
  const [selectedAction, setSelectedAction] = useState<string | null>(null);
  const [playerName, setPlayerName] = useState("");
  const [reason, setReason] = useState("");
  const [duration, setDuration] = useState("");
  const [customCommand, setCustomCommand] = useState("");
  const [executing, setExecuting] = useState(false);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; msg: string } | null>(null);

  const [logs, setLogs] = useState<ModerationLog[]>([]);
  const [logsLoading, setLogsLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [historySearch, setHistorySearch] = useState("");

  const loadLogs = () => {
    setLogsLoading(true);
    const url = `/api/admin/moderation?page=${page}${historySearch ? `&playerName=${encodeURIComponent(historySearch)}` : ""}`;
    fetch(url)
      .then((r) => r.json())
      .then((data) => {
        setLogs(data.logs || []);
        setTotalPages(data.totalPages || 1);
        setTotal(data.total || 0);
        setLogsLoading(false);
      })
      .catch(() => setLogsLoading(false));
  };

  useEffect(() => {
    const timer = setTimeout(() => {
        loadLogs();
    }, 400);
    return () => clearTimeout(timer);
  }, [page, historySearch]);

  const activeAction = ACTIONS.find((a) => a.id === selectedAction);

  const handleExecute = async () => {
    if (!selectedAction) return;
    setExecuting(true);
    setFeedback(null);

    try {
      const res = await fetch("/api/admin/moderation", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: selectedAction,
          playerName: playerName || undefined,
          reason: reason || undefined,
          duration: duration || undefined,
          customCommand: customCommand || undefined,
        }),
      });
      const data = await res.json();
      if (data.success) {
        setFeedback({ type: "success", msg: `✓ ${data.label} — command queued for execution.` });
        setPlayerName("");
        setReason("");
        setDuration("");
        setCustomCommand("");
        setSelectedAction(null);
        loadLogs();
      } else {
        setFeedback({ type: "error", msg: data.error || "Failed to execute action." });
      }
    } catch {
      setFeedback({ type: "error", msg: "Network error. Please try again." });
    } finally {
      setExecuting(false);
    }
  };

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold">
        <span className="bg-gradient-to-r from-red-400 to-orange-400 bg-clip-text text-transparent">Moderation</span>
        <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">Server management tools</span>
      </h1>

      {/* Feedback Toast */}
      {feedback && (
        <div
          className={`p-4 rounded-xl border text-sm font-medium animate-in fade-in slide-in-from-top-2 duration-300 ${
            feedback.type === "success"
              ? "bg-emerald-400/10 border-emerald-400/20 text-emerald-400"
              : "bg-red-400/10 border-red-400/20 text-red-400"
          }`}
        >
          {feedback.msg}
        </div>
      )}

      {/* Action Grid */}
      <div className="glass-card p-6">
        <h2 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">Quick Actions</h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {ACTIONS.map((action) => (
            <button
              key={action.id}
              onClick={() => {
                setSelectedAction(selectedAction === action.id ? null : action.id);
                setFeedback(null);
              }}
              className={`relative flex flex-col items-center gap-2 p-4 rounded-xl border transition-all duration-200 group ${
                selectedAction === action.id
                  ? `${actionColorMap[action.id]} shadow-lg scale-[1.02]`
                  : "bg-white/[0.03] border-white/10 text-[var(--text-secondary)] hover:bg-white/[0.06] hover:text-white hover:border-white/20"
              }`}
            >
              <span className="text-2xl group-hover:scale-110 transition-transform">{action.icon}</span>
              <span className="text-xs font-bold uppercase tracking-wider">{action.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Action Form */}
      {activeAction && (
        <div className="glass-card p-6 animate-in fade-in slide-in-from-top-2 duration-300">
          <h2 className="text-lg font-bold text-white mb-1 flex items-center gap-2">
            <span>{activeAction.icon}</span>
            {activeAction.label} Player
          </h2>
          <p className="text-xs text-[var(--text-muted)] mb-6">
            This command will be queued and executed on the server within ~5 seconds.
          </p>

          <div className="space-y-4 max-w-lg">
            {activeAction.requiresPlayer && (
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1.5 font-medium">Player Name</label>
                <input
                  type="text"
                  placeholder="Enter exact in-game name..."
                  value={playerName}
                  onChange={(e) => setPlayerName(e.target.value)}
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-white placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--accent)]/50 focus:border-[var(--accent)]/50 transition-all"
                />
              </div>
            )}

            {activeAction.hasDuration && (
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1.5 font-medium">Duration</label>
                <input
                  type="text"
                  placeholder="e.g. 30m, 6h, 7d, 30d"
                  value={duration}
                  onChange={(e) => setDuration(e.target.value)}
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-white placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--accent)]/50 focus:border-[var(--accent)]/50 transition-all"
                />
              </div>
            )}

            {activeAction.hasReason && (
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1.5 font-medium">
                  Reason <span className="text-[var(--text-muted)]">(optional)</span>
                </label>
                <input
                  type="text"
                  placeholder="Rule violation, griefing, etc."
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2.5 text-white placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--accent)]/50 focus:border-[var(--accent)]/50 transition-all"
                />
              </div>
            )}

            {activeAction.isCustom && (
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1.5 font-medium">Console Command</label>
                <div className="relative">
                  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--text-muted)] text-sm font-mono">/</span>
                  <input
                    type="text"
                    placeholder="say Hello World"
                    value={customCommand}
                    onChange={(e) => setCustomCommand(e.target.value)}
                    className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl pl-8 pr-4 py-2.5 text-white font-mono placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all"
                  />
                </div>
                <p className="text-[10px] text-[var(--text-muted)] mt-1.5">
                  ⚠️ Executes as console with full permissions. Use with caution.
                </p>
              </div>
            )}

            <button
              onClick={handleExecute}
              disabled={executing || (activeAction.requiresPlayer && !playerName) || (activeAction.isCustom && !customCommand)}
              className={`w-full py-3 rounded-xl font-bold text-sm uppercase tracking-wider transition-all disabled:opacity-30 disabled:cursor-not-allowed flex items-center justify-center gap-2 ${
                activeAction.id === "ban" || activeAction.id === "tempban"
                  ? "bg-red-500 hover:bg-red-600 text-white"
                  : "bg-gradient-to-r from-[var(--accent)] to-purple-600 text-white hover:shadow-lg"
              }`}
            >
              {executing ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Executing...
                </>
              ) : (
                <>Execute {activeAction.label}</>
              )}
            </button>
          </div>
        </div>
      )}

      {/* Moderation Logs */}
      <div className="glass-card overflow-hidden">
        <div className="p-6 border-b border-[var(--border)] flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h2 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider">
              Moderation Log
              <span className="ml-2 text-[var(--text-muted)] font-normal normal-case">{total} actions recorded</span>
            </h2>
          </div>
          
          <div className="relative group max-w-xs w-full">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="w-4 h-4 text-[var(--text-muted)] group-focus-within:text-red-400 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              placeholder="Search Player History..."
              value={historySearch}
              onChange={(e) => { setHistorySearch(e.target.value); setPage(1); }}
              className="w-full pl-10 pr-4 py-2 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-red-500/50 focus:border-red-500/50 transition-all font-medium"
            />
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)] bg-white/5">
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Action</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Player</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Duration</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Reason</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Admin</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider text-right">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {logsLoading && logs.length === 0 ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 6 }).map((_, j) => (
                      <td key={j} className="px-6 py-4"><div className="h-4 w-20 bg-white/5 rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-[var(--text-muted)] italic">
                    {historySearch ? `No records found for "${historySearch}"` : "No moderation actions recorded yet."}
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log._id} className="hover:bg-white/5 transition-colors">
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 text-[10px] font-black uppercase tracking-widest rounded-lg border ${actionColorMap[log.action] || "text-gray-400 bg-white/5 border-white/10"}`}>
                        {log.action}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-white font-medium">
                      {log.playerName || <span className="text-[var(--text-muted)]">—</span>}
                    </td>
                    <td className="px-6 py-4">
                      {log.duration ? (
                        <span className="text-[10px] font-mono font-bold bg-white/5 px-2 py-0.5 rounded text-[var(--text-secondary)] border border-white/5">
                          {log.duration}
                        </span>
                      ) : <span className="text-[var(--text-muted)]">—</span>}
                    </td>
                    <td className="px-6 py-4 text-[var(--text-secondary)] max-w-[200px] truncate">
                      {log.reason || <span className="text-[var(--text-muted)]">—</span>}
                    </td>
                    <td className="px-6 py-4 text-[var(--text-secondary)]">{log.adminName}</td>
                    <td className="px-6 py-4 text-[var(--text-muted)] text-xs tabular-nums text-right">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          page={page}
          total={total}
          totalPages={totalPages}
          limit={20}
          onPageChange={setPage}
          loading={logsLoading}
        />
      </div>
    </div>
  );
}
