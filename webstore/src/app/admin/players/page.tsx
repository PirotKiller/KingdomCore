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
                      {player.lastKnownName || "Unknown"}
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
                      <button onClick={() => handleEdit(player)} className="text-xs px-3 py-1.5 rounded-lg bg-[var(--bg-secondary)] hover:bg-[var(--bg-card-hover)] transition-colors border border-[var(--border)]">
                        Edit
                      </button>
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
            <h3 className="text-xl font-bold text-white mb-4">Edit Player</h3>
            <p className="text-xs font-mono text-[var(--text-muted)] mb-6">{editing.uuid}</p>
            
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
    </div>
  );
}
