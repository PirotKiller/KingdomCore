"use client";

import { useState, useEffect } from "react";
import Pagination from "@/components/Pagination";

interface GameLog {
  _id: string;
  eventType: string;
  playerName: string;
  uuid: string;
  details: string;
  timestamp: string;
  source?: "GAME" | "WEB";
}

const SOURCE_COLORS: Record<string, string> = {
  GAME: "text-blue-400 bg-blue-400/10 border-blue-400/20",
  WEB: "text-emerald-400 bg-emerald-400/10 border-emerald-400/20",
};

const EVENT_COLORS: Record<string, string> = {
  JOIN: "text-emerald-400 bg-emerald-400/10 border-emerald-400/20",
  QUIT: "text-red-400 bg-red-400/10 border-red-400/20",
  CHAT: "text-blue-400 bg-blue-400/10 border-blue-400/20",
  COMMAND: "text-purple-400 bg-purple-400/10 border-purple-400/20",
  SHOP_PURCHASE: "text-emerald-400 bg-emerald-400/10 border-emerald-400/20",
  AH_BUY: "text-amber-400 bg-amber-400/10 border-amber-400/20",
  AH_LIST: "text-amber-300 bg-amber-300/10 border-amber-300/20",
  ECO_ADMIN: "text-blue-400 bg-blue-400/10 border-blue-400/20",
  ECO_WITHDRAW: "text-red-300 bg-red-300/10 border-red-300/20",
  ECO_DEPOSIT: "text-green-400 bg-green-400/10 border-green-400/20",
  BOUNTY_CLAIM: "text-orange-400 bg-orange-400/10 border-orange-400/20",
  CONVERTER_SELL: "text-yellow-400 bg-yellow-400/10 border-yellow-400/20",
  GEM_CONVERSION: "text-cyan-400 bg-cyan-400/10 border-cyan-400/20",
  ADMIN_ACTION: "text-fuchsia-400 bg-fuchsia-400/10 border-fuchsia-400/20",
  MODERATION: "text-rose-400 bg-rose-400/10 border-rose-400/20",
  LEVEL_UP: "text-yellow-300 bg-yellow-300/10 border-yellow-300/20 font-bold",
  CLASS_CHANGE: "text-indigo-400 bg-indigo-400/10 border-indigo-400/20",
  BOUNTY_SET: "text-red-500 bg-red-500/10 border-red-500/20",
  STORE_CHECKOUT_START: "text-slate-400 bg-slate-400/10 border-slate-400/20",
  STORE_PURCHASE_COMPLETE: "text-emerald-500 bg-emerald-500/10 border-emerald-500/20 font-bold",
  STORE_DELIVERY: "text-cyan-400 bg-cyan-400/10 border-cyan-400/20",
};

export default function AdminLogsPage() {
  const [logs, setLogs] = useState<GameLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState("");
  const [filterType, setFilterType] = useState("ALL");
  const [filterSource, setFilterSource] = useState("ALL");

  const loadLogs = (p: number, currentSearch: string, currentFilter: string, currentSource: string) => {
    setLoading(true);
    fetch(`/api/admin/logs?page=${p}&search=${encodeURIComponent(currentSearch)}&type=${currentFilter}&source=${currentSource}`)
      .then((r) => r.json())
      .then((data) => {
        setLogs(data.logs || []);
        setTotalPages(data.totalPages || 1);
        setTotal(data.total || 0);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    const timer = setTimeout(() => {
        loadLogs(page, search, filterType, filterSource);
    }, 300);
    return () => clearTimeout(timer);
  }, [page, search, filterType, filterSource]);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearch(e.target.value);
    setPage(1);
  };

  const handleFilterChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setFilterType(e.target.value);
    setPage(1);
  };

  const handleSourceChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setFilterSource(e.target.value);
    setPage(1);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">
          <span className="bg-gradient-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">Game Logs</span>
          <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{total} recorded events</span>
        </h1>

        <div className="flex flex-col sm:flex-row gap-3">
          <select 
            value={filterSource}
            onChange={handleSourceChange}
            className="bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50"
          >
            <option value="ALL">All Sources</option>
            <option value="GAME">Minecraft</option>
            <option value="WEB">WebStore</option>
          </select>

          <select 
            value={filterType}
            onChange={handleFilterChange}
            className="bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50"
          >
            <option value="ALL">All Events</option>
            <option value="LEVEL_UP">Level Ups</option>
            <option value="CLASS_CHANGE">Classes</option>
            <option value="BOUNTY_SET">Bounties Set</option>
            <option value="BOUNTY_CLAIM">Bounties Claimed</option>
            <option value="STORE_PURCHASE_COMPLETE">Store Sales</option>
            <option value="STORE_DELIVERY">Deliveries</option>
            <option value="STORE_CHECKOUT_START">Checkouts</option>
            <option value="MODERATION">Moderation</option>
            <option value="ECO_ADMIN">Eco Admin</option>
            <option value="JOIN">Joins</option>
            <option value="QUIT">Quits</option>
            <option value="CHAT">Chat</option>
          </select>

          
          <div className="relative group min-w-[250px]">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="w-4 h-4 text-[var(--text-muted)] group-focus-within:text-blue-400 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              placeholder="Search player or contents..."
              value={search}
              onChange={handleSearchChange}
              className="w-full pl-10 pr-4 py-2 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>
      </div>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)] bg-white/5">
                <th className="px-6 py-4 font-bold uppercase tracking-wider w-32">Type</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider w-48">Player</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider">Details</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider w-48 text-right">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)] font-mono">
              {loading ? (
                Array.from({ length: 10 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 4 }).map((_, j) => (
                      <td key={j} className="px-6 py-3"><div className="h-4 w-full bg-white/5 rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-6 py-12 text-center text-[var(--text-muted)] italic font-sans">
                    No logs found.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log._id} className="hover:bg-white/5 transition-colors">
                    <td className="px-6 py-3">
                      <div className="flex items-center gap-2">
                        <span className={`px-2 py-0.5 text-[9px] font-bold rounded border ${SOURCE_COLORS[log.source || "GAME"]}`}>
                          {log.source || "GAME"}
                        </span>
                        <span className={`px-2 py-1 text-[10px] font-black uppercase tracking-widest rounded border ${EVENT_COLORS[log.eventType] || "text-gray-400 bg-white/5 border-white/10"}`}>
                          {log.eventType}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-3 text-white">
                      {log.playerName}
                    </td>
                    <td className="px-6 py-3 text-[var(--text-secondary)] whitespace-pre-wrap break-all">
                      {log.details}
                    </td>
                    <td className="px-6 py-3 text-[var(--text-muted)] text-xs text-right tabular-nums">
                      {new Date(log.timestamp).toLocaleString()}
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
          limit={40}
          onPageChange={setPage}
          loading={loading}
        />
      </div>
    </div>
  );
}
