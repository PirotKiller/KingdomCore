"use client";

import { useEffect, useState } from "react";

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

  useEffect(() => {
    fetch("/api/admin/users")
      .then((r) => r.json())
      .then((data) => { setUsers(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">
        <span className="bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">Users</span>
        <span className="ml-3 text-sm text-[var(--text-muted)] font-normal">{users.length} total</span>
      </h1>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] text-left text-[var(--text-muted)]">
                <th className="px-4 py-3 font-medium">Discord</th>
                <th className="px-4 py-3 font-medium">Minecraft</th>
                <th className="px-4 py-3 font-medium">Role</th>
                <th className="px-4 py-3 font-medium">Joined</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-[var(--border)]">
                    <td className="px-4 py-3"><div className="h-4 w-32 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-24 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-16 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                    <td className="px-4 py-3"><div className="h-4 w-20 bg-[var(--bg-secondary)] rounded animate-pulse" /></td>
                  </tr>
                ))
              ) : (
                users.map((user) => (
                  <tr key={user._id} className="border-b border-[var(--border)] hover:bg-[var(--bg-card-hover)] transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {user.discordAvatar && (
                          <img src={user.discordAvatar} alt="" className="w-6 h-6 rounded-full" />
                        )}
                        <span className="text-white">{user.discordUsername}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {user.minecraftUsername ? (
                        <span className="text-emerald-400">{user.minecraftUsername}</span>
                      ) : (
                        <span className="text-[var(--text-muted)]">Not linked</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {user.isAdmin ? (
                        <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-amber-400/10 text-amber-400 border border-amber-400/20">Admin</span>
                      ) : (
                        <span className="text-[var(--text-muted)]">User</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-[var(--text-muted)]">
                      {new Date(user.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
