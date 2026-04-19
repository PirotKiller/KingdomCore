"use client";

import { useEffect, useState } from "react";
import Pagination from "@/components/Pagination";

interface PlayerData {
  _id: string;
  uuid: string;
  class: string;
  shards: number;
  gems: number;
  xp: number;
  level: number;
  bounty: number;
  online: boolean;
  lastKnownName: string;
}

export default function AdminPlayersPage() {
  const [players, setPlayers] = useState<PlayerData[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<PlayerData | null>(null);
  const [form, setForm] = useState({ shards: 0, gems: 0, level: 1, bounty: 0 });
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [viewingHistory, setViewingHistory] = useState<string | null>(null);
  const [historyData, setHistoryData] = useState<any>(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyTab, setHistoryTab] = useState<"mod" | "game" | "shop">("mod");

  const loadPlayers = () => {
    setLoading(true);
    fetch(`/api/admin/players?page=${page}`)
      .then((r) => r.json())
      .then((data) => { 
        setPlayers(data.players || []); 
        setTotalPages(data.totalPages || 1);
        setTotal(data.total || 0);
        setLoading(false); 
      })
      .catch(() => setLoading(false));
  };

  useEffect(loadPlayers, [page]);

  const handleEdit = (player: PlayerData) => {
    setEditing(player);
    setForm({ shards: player.shards, gems: player.gems, level: player.level, bounty: player.bounty });
  };

  const handleSave = async () => {
    if (!editing) return;
    await fetch("/api/admin/players", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ uuid: editing.uuid, ...form }),
    });
    setEditing(null);
    loadPlayers();
  };

  const handleOpenHistory = async (playerName: string) => {
    setViewingHistory(playerName);
    setHistoryLoading(true);
    setHistoryTab("mod");
    try {
      const res = await fetch(`/api/admin/players/${playerName}/history`);
      const data = await res.json();
      setHistoryData(data);
    } catch (e) {
      console.error(e);
    } finally {
      setHistoryLoading(false);
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">
        <span className="bg-gradient-to-r from-emerald-400 to-cyan-400 bg-clip-text text-transparent">In-Game Players</span>
        <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{total} synced</span>
      </h1>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Player Name</th>
                <th className="px-4 py-3 font-medium">Player UUID</th>
                <th className="px-4 py-3 font-medium">Class</th>
                <th className="px-4 py-3 font-medium">Level</th>
                <th className="px-4 py-3 font-medium">Shards</th>
                <th className="px-4 py-3 font-medium">Gems</th>
                <th className="px-4 py-3 font-medium">Bounty</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-[var(--border)]">
                    <td className="px-4 py-3"><div className="h-4 w-4 bg-[var(--bg-secondary)] rounded-full animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-24 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-32 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-16 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-12 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-16 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-16 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-16 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-12 bg-[var(--bg-secondary)] rounded animate-pulse ml-auto" /></td>
                  </tr>
                ))
              ) : players.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-[var(--text-muted)]">
                    No players found in database.
                  </td>
                </tr>
              ) : (
                players.map((player) => (
                  <tr key={player._id} className="border-b border-[var(--border)] hover:bg-[var(--bg-card-hover)] transition-colors">
                    <td className="px-4 py-3">
                      <span className={`inline-block w-3 h-3 rounded-full shadow-lg ${player.online ? "bg-emerald-400 shadow-emerald-400/50" : "bg-red-500 shadow-red-500/50"}`} title={player.online ? "Online" : "Offline"} />
                    </td>
                    <td className="px-4 py-3 font-bold text-white">
                      <div className="flex items-center gap-3">
                        <img 
                          src={`https://mc-heads.net/avatar/${player.uuid}/32`} 
                          alt="" 
                          className="w-8 h-8 rounded-md bg-[var(--bg-secondary)] border border-[var(--border)]"
                        />
                        <span>{player.lastKnownName || "Unknown"}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-[var(--text-secondary)]">
                      {player.uuid}
                    </td>
                    <td className="px-4 py-3">
                      <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                        {player.class || "None"}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-bold text-white">
                      {player.level || 1}
                    </td>
                    <td className="px-4 py-3 text-emerald-400 font-medium">
                      {player.shards || 0}
                    </td>
                    <td className="px-4 py-3 text-cyan-400 font-medium">
                      {player.gems || 0}
                    </td>
                    <td className="px-4 py-3 text-amber-400 font-medium">
                      {player.bounty || 0}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <button onClick={() => handleOpenHistory(player.lastKnownName)} className="text-xs px-3 py-1.5 rounded-lg bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border border-blue-500/20 transition-colors">
                          History
                        </button>
                        <button onClick={() => handleEdit(player)} className="text-xs px-3 py-1.5 rounded-lg bg-[var(--bg-secondary)] hover:bg-[var(--bg-card-hover)] transition-colors border border-[var(--border)]">
                          Edit
                        </button>
                      </div>
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
          loading={loading}
        />
      </div>

      {/* Edit Modal */}
      {editing && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="glass-card w-full max-w-md p-6 border border-[var(--border)] overflow-y-auto max-h-[90vh]">
            <div className="flex items-center gap-4 mb-6">
              <img src={`https://mc-heads.net/avatar/${editing.uuid}/48`} alt="" className="w-12 h-12 rounded-lg bg-[var(--bg-secondary)] border border-[var(--border)] shadow-lg" />
              <div>
                <h3 className="text-xl font-bold text-white leading-tight">{editing.lastKnownName}</h3>
                <p className="text-[10px] font-mono text-[var(--text-muted)] mt-1">{editing.uuid}</p>
              </div>
            </div>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1">Level</label>
                <input type="number" value={form.level} onChange={e => setForm({...form, level: Number(e.target.value)})} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded px-3 py-2 text-white" />
              </div>
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1">Shards</label>
                <input type="number" value={form.shards} onChange={e => setForm({...form, shards: Number(e.target.value)})} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded px-3 py-2 text-emerald-400 font-bold" />
              </div>
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1">Gems</label>
                <input type="number" value={form.gems} onChange={e => setForm({...form, gems: Number(e.target.value)})} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded px-3 py-2 text-cyan-400 font-bold" />
              </div>
              <div>
                <label className="block text-sm text-[var(--text-secondary)] mb-1">Bounty</label>
                <input type="number" value={form.bounty} onChange={e => setForm({...form, bounty: Number(e.target.value)})} className="w-full bg-[var(--bg-secondary)] border border-[var(--border)] rounded px-3 py-2 text-amber-400 font-bold" />
              </div>
            </div>

            <div className="flex gap-3 justify-end mt-8">
              <button onClick={() => setEditing(null)} className="px-4 py-2 rounded-lg text-sm font-medium hover:bg-[var(--bg-secondary)] transition-colors">Cancel</button>
              <button onClick={handleSave} className="px-4 py-2 rounded-lg text-sm font-medium bg-emerald-500 hover:bg-emerald-600 text-white transition-colors">Save Changes</button>
            </div>
          </div>
        </div>
      )}

      {/* History Modal */}
      {viewingHistory && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 z-50">
          <div className="glass-card w-full max-w-4xl h-[80vh] flex flex-col border border-white/10 shadow-2xl animate-in zoom-in-95 duration-200">
            {/* Header */}
            <div className="p-6 border-b border-white/10 flex items-center justify-between">
              <div className="flex items-center gap-4 text-left">
                <div className="w-12 h-12 rounded-xl bg-[var(--bg-secondary)] flex items-center justify-center overflow-hidden border border-white/10 shadow-lg shadow-black/50">
                  <img src={`https://mc-heads.net/avatar/${viewingHistory}/48`} alt="" className="w-full h-full object-cover" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-white flex items-center gap-2">
                    {viewingHistory}
                    <span className="text-xs font-normal text-[var(--text-muted)] bg-white/5 px-2 py-0.5 rounded border border-white/5">Character Profile</span>
                  </h3>
                  {historyData?.linkedUser && (
                    <p className="text-xs text-blue-400 flex items-center gap-1.5 mt-0.5">
                      <span className="opacity-70">Discord:</span> {historyData.linkedUser.discordUsername}
                    </p>
                  )}
                </div>
              </div>
              <button 
                onClick={() => { setViewingHistory(null); setHistoryData(null); }}
                className="w-10 h-10 rounded-xl bg-white/5 hover:bg-white/10 flex items-center justify-center transition-colors border border-white/10 text-white"
              >
                ✕
              </button>
            </div>

            {/* Tabs */}
            <div className="flex border-b border-white/5 px-6">
              {[
                { id: 'mod', label: 'Moderation Logs', icon: '🛡️' },
                { id: 'game', label: 'Game Activity', icon: '📜' },
                { id: 'shop', label: 'Purchase History', icon: '🛒' }
              ].map(tab => (
                <button
                  key={tab.id}
                  onClick={() => setHistoryTab(tab.id as any)}
                  className={`px-6 py-4 text-xs font-black uppercase tracking-widest transition-all border-b-2 flex items-center gap-2 ${
                    historyTab === tab.id 
                      ? 'text-blue-400 border-blue-400 bg-blue-400/5' 
                      : 'text-[var(--text-muted)] border-transparent hover:text-white hover:bg-white/5'
                  }`}
                >
                  <span>{tab.icon}</span> {tab.label}
                </button>
              ))}
            </div>

            {/* Content Container */}
            <div className="flex-1 overflow-y-auto p-6 font-sans">
              {historyLoading ? (
                <div className="h-full flex flex-col items-center justify-center space-y-4">
                  <div className="w-12 h-12 border-4 border-blue-500/20 border-t-blue-500 rounded-full animate-spin" />
                  <p className="text-sm text-[var(--text-muted)] animate-pulse">Retrieving archived player data...</p>
                </div>
              ) : !historyData ? (
                <p className="text-center text-[var(--text-muted)] py-20 italic">No data available for this player.</p>
              ) : (
                <div className="animate-in fade-in duration-300">
                  {/* Tab Contents */}
                  {historyTab === 'mod' && (
                    <div className="space-y-3">
                      {historyData.moderationLogs?.length === 0 ? (
                        <p className="text-center text-[var(--text-muted)] py-10 italic">Clean record. No moderation actions found.</p>
                      ) : (
                        historyData.moderationLogs.map((log: any) => (
                          <div key={log._id} className="p-4 rounded-xl bg-white/5 border border-white/5 hover:border-white/10 transition-colors flex items-center justify-between">
                            <div className="flex items-center gap-4 text-left">
                              <span className={`px-2 py-0.5 text-[9px] font-black uppercase tracking-widest rounded border ${
                                  log.action === 'ban' || log.action === 'tempban' ? 'text-red-400 bg-red-400/10 border-red-400/20' :
                                  log.action === 'kick' ? 'text-amber-400 bg-amber-400/10 border-amber-400/20' :
                                  'text-blue-400 bg-blue-400/10 border-blue-400/20'
                              }`}>
                                {log.action}
                              </span>
                              <div>
                                <p className="text-sm text-white font-medium">{log.reason || "No reason specified"}</p>
                                <p className="text-[10px] text-[var(--text-muted)] mt-0.5 flex items-center gap-1.5">
                                  <span>Admin: {log.adminName}</span>
                                  {log.duration && <span className="text-blue-400/80">• Duration: {log.duration}</span>}
                                </p>
                              </div>
                            </div>
                            <span className="text-[10px] text-[var(--text-muted)] tabular-nums">{new Date(log.createdAt).toLocaleString()}</span>
                          </div>
                        ))
                      )}
                    </div>
                  )}

                  {historyTab === 'game' && (
                    <div className="space-y-1.5">
                      {historyData.gameLogs?.length === 0 ? (
                        <p className="text-center text-[var(--text-muted)] py-10 italic">No recorded game activity.</p>
                      ) : (
                        historyData.gameLogs.map((log: any) => (
                          <div key={log._id} className="p-2.5 rounded-lg bg-white/5 border border-transparent hover:border-white/10 transition-colors flex items-baseline gap-4 group text-left">
                             <span className={`px-1.5 py-0.5 text-[8px] font-black uppercase tracking-tighter rounded shrink-0 ${
                                log.eventType === 'CHAT' ? 'text-emerald-400 bg-emerald-400/10' :
                                log.eventType === 'COMMAND' ? 'text-purple-400 bg-purple-400/10' :
                                'text-gray-400 bg-white/5'
                             }`}>
                                {log.eventType}
                             </span>
                             <p className="text-xs text-[var(--text-secondary)] font-mono break-all leading-relaxed group-hover:text-white transition-colors">
                                {log.details}
                             </p>
                             <span className="ml-auto text-[9px] text-[var(--text-muted)] shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
                                {new Date(log.timestamp).toLocaleTimeString()}
                             </span>
                          </div>
                        ))
                      )}
                      <p className="text-[10px] text-[var(--text-muted)] text-center pt-4">Showing the last 100 game events.</p>
                    </div>
                  )}

                  {historyTab === 'shop' && (
                    <div className="space-y-3 font-sans">
                      {historyData.purchases?.length === 0 ? (
                        <p className="text-center text-[var(--text-muted)] py-10 italic">No store transactions found for this player.</p>
                      ) : (
                        historyData.purchases.map((p: any) => (
                          <div key={p._id} className="p-4 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between">
                            <div className="flex items-center gap-4 text-left">
                              <div className="w-10 h-10 rounded-lg bg-emerald-500/10 flex items-center justify-center text-lg">💎</div>
                              <div>
                                <p className="text-sm text-white font-bold">{p.itemName}</p>
                                <p className="text-[10px] text-blue-400 font-mono tracking-wider">{p.transactionId || "LEGACY"}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <p className="text-emerald-400 font-bold text-sm text-right">${(p.price / 100).toFixed(2)}</p>
                              <p className="text-[10px] text-[var(--text-muted)] text-right">{new Date(p.createdAt).toLocaleDateString()}</p>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="p-4 border-t border-white/10 bg-white/[0.02] flex items-center justify-center">
              <p className="text-[10px] text-[var(--text-muted)] uppercase tracking-widest font-bold">
                KingdomCore Unified Intelligence View
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
