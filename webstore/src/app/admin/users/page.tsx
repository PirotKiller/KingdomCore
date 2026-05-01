"use client";

import { useEffect, useState } from "react";
import Pagination from "@/components/Pagination";

interface User {
  _id: string;
  discordId: string;
  discordUsername: string;
  discordAvatar?: string;
  minecraftUuid?: string;
  minecraftUsername?: string;
  isAdmin: boolean;
  createdAt: string;
}

export default function AdminUsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [toggling, setToggling] = useState<string | null>(null);

  const fetchUsers = () => {
    setLoading(true);
    fetch(`/api/admin/users?search=${encodeURIComponent(search)}&page=${page}`)
      .then((r) => r.json())
      .then((data) => {
        setUsers(data.users || []);
        setTotalPages(data.totalPages || 1);
        setTotal(data.total || 0);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchUsers();
    }, 300);
    return () => clearTimeout(timer);
  }, [search, page]);

  const toggleAdmin = async (userId: string, currentStatus: boolean) => {
    if (toggling) return;
    setToggling(userId);
    try {
      const res = await fetch(`/api/admin/users/${userId}/toggle-admin`, { method: "POST" });
      const data = await res.json();
      if (data.success) {
        setUsers(prev => prev.map(u => u._id === userId ? { ...u, isAdmin: data.isAdmin } : u));
      } else {
        alert(data.error || "Failed to update role");
      }
    } catch (err) {
      alert("Error updating role");
    } finally {
      setToggling(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">
          <span className="bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">Users</span>
          <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{total} total</span>
        </h1>

        <div className="relative group max-w-md w-full">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg className="w-4 h-4 text-[var(--text-muted)] group-focus-within:text-purple-400 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <input
            type="text"
            placeholder="Search Discord or Minecraft username..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            className="w-full pl-10 pr-4 py-2 bg-[var(--bg-secondary)] border border-[var(--border)] rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500/50 transition-all"
          />
        </div>
      </div>

      {/* Mobile View: Cards */}
      <div className="grid grid-cols-1 gap-4 md:hidden">
        {loading && users.length === 0 ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="glass-card p-5 h-32 animate-pulse bg-white/5 border-white/10" />
          ))
        ) : users.length === 0 ? (
          <div className="glass-card p-10 text-center text-[var(--text-muted)] italic">
            No users found
          </div>
        ) : (
          users.map((user) => (
            <div key={user._id} className="glass-card p-5 border-white/5 space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <img 
                    src={user.discordAvatar || "https://cdn.discordapp.com/embed/avatars/0.png"} 
                    alt="" 
                    className="w-10 h-10 rounded-full border border-white/10" 
                  />
                  <div>
                    <div className="text-white font-bold text-sm">{user.discordUsername}</div>
                    <div className="text-[10px] text-[var(--text-muted)] uppercase tracking-tighter">{user.discordId}</div>
                  </div>
                </div>
                <button
                  onClick={() => toggleAdmin(user._id, user.isAdmin)}
                  disabled={toggling !== null}
                  className={`group px-3 py-1 rounded-lg border transition-all flex items-center gap-2 ${
                    user.isAdmin 
                      ? "bg-amber-400/10 border-amber-400/50 text-amber-400" 
                      : "bg-white/5 border-white/10 text-[var(--text-muted)]"
                  }`}
                >
                  <span className={`w-1.5 h-1.5 rounded-full ${user.isAdmin ? "bg-amber-400 animate-pulse" : "bg-gray-600"}`} />
                  <span className="text-[10px] font-black uppercase tracking-widest">{user.isAdmin ? "Admin" : "User"}</span>
                </button>
              </div>

              <div className="flex items-center justify-between py-3 border-t border-b border-white/5">
                <div className="flex flex-col">
                  <span className="text-[9px] font-black text-[var(--text-muted)] uppercase tracking-[0.2em] mb-1">Minecraft</span>
                  {user.minecraftUsername ? (
                    <div className="flex items-center gap-2">
                      <img src={`https://mc-heads.net/avatar/${user.minecraftUsername}/24`} alt="" className="w-4 h-4 rounded" />
                      <span className="text-emerald-400 text-xs font-bold">{user.minecraftUsername}</span>
                    </div>
                  ) : (
                    <span className="text-white/20 text-xs italic">Not linked</span>
                  )}
                </div>
                <div className="text-right">
                  <span className="text-[9px] font-black text-[var(--text-muted)] uppercase tracking-[0.2em] mb-1 block">Joined</span>
                  <span className="text-[var(--text-secondary)] text-xs tabular-nums">{new Date(user.createdAt).toLocaleDateString()}</span>
                </div>
              </div>
            </div>
          )))
        }
      </div>

      {/* Desktop View: Table */}
      <div className="hidden md:block glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)] bg-white/5">
                <th className="px-6 py-4 font-bold uppercase tracking-wider text-[10px]">Discord</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider text-[10px]">Minecraft</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider text-[10px]">Role</th>
                <th className="px-6 py-4 font-bold uppercase tracking-wider text-[10px]">Joined</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {loading && users.length === 0 ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td className="px-6 py-4"><div className="h-4 w-32 bg-white/5 rounded animate-pulse" /></td>
                    <td className="px-6 py-4"><div className="h-4 w-24 bg-white/5 rounded animate-pulse" /></td>
                    <td className="px-6 py-4"><div className="h-4 w-16 bg-white/5 rounded animate-pulse" /></td>
                    <td className="px-6 py-4"><div className="h-4 w-20 bg-white/5 rounded animate-pulse" /></td>
                  </tr>
                ))
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-6 py-12 text-center text-[var(--text-muted)] italic">
                    No users found
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user._id} className="hover:bg-white/5 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <img 
                          src={user.discordAvatar || "https://cdn.discordapp.com/embed/avatars/0.png"} 
                          alt="" 
                          className="w-8 h-8 rounded-full border border-white/10" 
                        />
                        <span className="text-white font-medium">{user.discordUsername}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {user.minecraftUsername ? (
                        <div className="flex items-center gap-2">
                          <img src={`https://mc-heads.net/avatar/${user.minecraftUsername}/24`} alt="" className="w-5 h-5 rounded" />
                          <span className="text-emerald-400 font-bold">{user.minecraftUsername}</span>
                        </div>
                      ) : (
                        <span className="text-[var(--text-muted)] text-[10px] uppercase tracking-tight">Not linked</span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <button
                        onClick={() => toggleAdmin(user._id, user.isAdmin)}
                        disabled={toggling !== null}
                        className={`group px-3 py-1 rounded-lg border transition-all flex items-center gap-2 ${
                          user.isAdmin 
                            ? "bg-amber-400/10 border-amber-400/50 text-amber-400 hover:bg-amber-400/20" 
                            : "bg-white/5 border-white/10 text-[var(--text-muted)] hover:text-white hover:border-white/20"
                        }`}
                      >
                        <span className={`w-1.5 h-1.5 rounded-full ${user.isAdmin ? "bg-amber-400 animate-pulse" : "bg-gray-600 group-hover:bg-gray-400"}`} />
                        <span className="text-[10px] font-black uppercase tracking-widest">{user.isAdmin ? "Admin" : "User"}</span>
                        {toggling === user._id && <div className="w-3 h-3 border-2 border-current border-t-transparent rounded-full animate-spin" />}
                      </button>
                    </td>
                    <td className="px-6 py-4 text-[var(--text-muted)] tabular-nums text-xs">
                      {new Date(user.createdAt).toLocaleDateString()}
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
    </div>
  );
}
