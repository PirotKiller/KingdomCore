"use client";

import { useEffect, useState } from "react";
import useSWR from "swr";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  Cell,
  LineChart,
  Line,
} from "recharts";

const fetcher = (url: string) => fetch(url).then((r) => r.json());

interface Stats {
  totalUsers: number;
  verifiedUsers: number;
  totalInGamePlayers: number;
  onlinePlayers: number;
  totalPurchases: number;
  totalRevenue: number;
  totalBounty: number;
  activeItems: number;
  activeAuctions: number;
  recentPurchases: any[];
}

interface ChartData {
  date: string;
  revenue: number;
  sales: number;
  growth: number;
}

export default function AdminDashboard() {
  const { data: stats } = useSWR<Stats>("/api/admin/stats", fetcher, {
    refreshInterval: 5000,
  });

  const { data: chartData } = useSWR<ChartData[]>("/api/admin/stats/charts", fetcher, {
    refreshInterval: 30000,
  });

  const cards = stats
    ? [
        { label: "Online Players", value: stats.onlinePlayers, icon: "🟢", color: "text-emerald-400" },
        { label: "Total Revenue", value: `$${(stats.totalRevenue / 100).toFixed(2)}`, icon: "💰", color: "text-amber-400" },
        { label: "Daily Sales", value: stats.totalPurchases, icon: "💳", color: "text-indigo-400" },
        { label: "New Players (Web)", value: stats.totalUsers, icon: "👥", color: "text-cyan-400" },
      ]
    : [];

  return (
    <div className="space-y-10 pb-16">
      <div>
        <h1 className="text-3xl font-black tracking-tighter mb-8 leading-none">
          <span className="bg-gradient-to-r from-amber-400 to-orange-500 bg-clip-text text-transparent">ADMIN</span>
          <span className="text-white ml-2">DASHBOARD</span>
        </h1>

        {/* Quick Stats Grid */}
        {!stats ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="glass-card p-6 h-32 animate-pulse bg-white/5 border-white/10" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {cards.map((card) => (
              <div key={card.label} className="glass-card p-6 border-white/5 hover:border-white/20 transition-all hover:-translate-y-1">
                <div className="flex items-center justify-between mb-4">
                  <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center text-xl shadow-inner">
                    {card.icon}
                  </div>
                  <span className={`text-2xl font-black ${card.color}`}>{card.value}</span>
                </div>
                <div className="text-[10px] font-black uppercase tracking-[0.2em] text-[var(--text-muted)]">{card.label}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Analytics Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Revenue Chart */}
        <div className="glass-card p-5 border-white/5 relative overflow-hidden group">
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-purple-500 to-transparent opacity-30" />
          <h3 className="text-sm font-black uppercase tracking-[0.2em] text-[var(--text-secondary)] mb-8 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-purple-500 animate-pulse" />
            Revenue Performance (Last 14 Days)
          </h3>
          
          <div className="h-[300px] w-full flex items-center justify-center">
            {chartData && (chartData as any).error ? (
              <div className="text-[var(--text-muted)] text-sm font-bold bg-white/5 px-4 py-2 rounded-xl border border-white/10 uppercase tracking-widest">
                {(chartData as any).error}
              </div>
            ) : !Array.isArray(chartData) ? (
              <div className="h-full w-full bg-white/5 animate-pulse rounded-2xl" />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData}>
                  <defs>
                    <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                  <XAxis 
                    dataKey="date" 
                    stroke="rgba(255,255,255,0.3)" 
                    fontSize={10} 
                    tickFormatter={(str) => str.split('-').slice(1).join('/')}
                    axisLine={false}
                    tickLine={false}
                    dy={10}
                  />
                  <YAxis 
                    stroke="rgba(255,255,255,0.3)" 
                    fontSize={10}
                    axisLine={false}
                    tickLine={false}
                    tickFormatter={(val) => `$${val}`}
                  />
                  <Tooltip 
                    contentStyle={{ borderRadius: '16px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(18,18,26,0.95)', backdropFilter: 'blur(10px)' }} 
                    itemStyle={{ color: '#8b5cf6', fontSize: '13px', fontWeight: 'bold' }}
                    labelStyle={{ fontSize: '11px', color: '#94a3b8', marginBottom: '4px' }}
                  />
                  <Area type="monotone" dataKey="revenue" stroke="#8b5cf6" strokeWidth={3} fillOpacity={1} fill="url(#colorRev)" />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Player Growth Chart */}
        <div className="glass-card p-5 border-white/5 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-cyan-500 to-transparent opacity-30" />
          <h3 className="text-sm font-black uppercase tracking-[0.2em] text-[var(--text-secondary)] mb-8 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-cyan-500 animate-pulse" />
            Player Registrations
          </h3>
          
          <div className="h-[300px] w-full flex items-center justify-center">
            {chartData && (chartData as any).error ? (
              <div className="text-[var(--text-muted)] text-sm font-bold bg-white/5 px-4 py-2 rounded-xl border border-white/10 uppercase tracking-widest">
                {(chartData as any).error}
              </div>
            ) : !Array.isArray(chartData) ? (
              <div className="h-full w-full bg-white/5 animate-pulse rounded-2xl" />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <defs>
                    <filter id="cyanGlow" x="-20%" y="-20%" width="140%" height="140%">
                      <feGaussianBlur stdDeviation="3" result="blur" />
                      <feComposite in="SourceGraphic" in2="blur" operator="over" />
                    </filter>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                  <XAxis 
                    dataKey="date" 
                    stroke="rgba(255,255,255,0.3)" 
                    fontSize={10} 
                    tickFormatter={(str) => str.split('-').slice(2).join('')}
                    axisLine={false}
                    tickLine={false}
                    dy={10}
                  />
                  <YAxis 
                    stroke="rgba(255,255,255,0.3)" 
                    fontSize={10}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip 
                    contentStyle={{ borderRadius: '16px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(18,18,26,0.95)' }} 
                    itemStyle={{ color: '#06b6d4', fontSize: '13px', fontWeight: 'bold' }}
                    labelStyle={{ fontSize: '11px', color: '#94a3b8', marginBottom: '4px' }}
                  />
                  <Line type="monotone" dataKey="growth" stroke="#06b6d4" strokeWidth={4} dot={{ r: 4, fill: '#06b6d4', strokeWidth: 0 }} activeDot={{ r: 6, strokeWidth: 0 }} filter="url(#cyanGlow)" />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Daily Transactions Bar Chart */}
        <div className="glass-card p-5 border-white/5 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-emerald-500 to-transparent opacity-30" />
          <h3 className="text-sm font-black uppercase tracking-[0.2em] text-[var(--text-secondary)] mb-8 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-500" />
            Daily Transactions
          </h3>
          
          <div className="h-[300px] w-full flex items-center justify-center">
            {chartData && (chartData as any).error ? (
              <div className="text-[var(--text-muted)] text-sm font-bold bg-white/5 px-4 py-2 rounded-xl border border-white/10 uppercase tracking-widest">
                {(chartData as any).error}
              </div>
            ) : !Array.isArray(chartData) ? (
              <div className="h-full w-full bg-white/5 animate-pulse rounded-2xl" />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                  <XAxis 
                    dataKey="date" 
                    stroke="rgba(255,255,255,0.3)" 
                    fontSize={10} 
                    tickFormatter={(str) => str.split('-').slice(2).join('')}
                    axisLine={false}
                    tickLine={false}
                    dy={10}
                  />
                  <YAxis 
                    stroke="rgba(255,255,255,0.3)" 
                    fontSize={10}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip 
                    cursor={{fill: 'rgba(255,255,255,0.03)'}}
                    contentStyle={{ borderRadius: '16px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(18,18,26,0.95)' }} 
                    itemStyle={{ color: '#10b981', fontSize: '13px', fontWeight: 'bold' }}
                    labelStyle={{ fontSize: '11px', color: '#94a3b8', marginBottom: '4px' }}
                  />
                  <Bar dataKey="sales" fill="#10b981" radius={[4, 4, 0, 0]}>
                    {Array.isArray(chartData) && chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.sales > 0 ? '#10b981' : 'rgba(255,255,255,0.05)'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* Recent Activity Table */}
      <div>
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-2xl font-black text-white tracking-tight flex items-center gap-3">
             <span className="w-5 h-5 bg-amber-400 rounded-lg flex items-center justify-center text-xs">🕒</span>
             RECENT PURCHASES
          </h2>
          <span className="px-4 py-1.5 rounded-full bg-white/5 border border-white/10 text-[10px] font-black uppercase tracking-widest text-[var(--text-muted)] animate-pulse">
            Live Stream
          </span>
        </div>
        
        <div className="glass-card overflow-hidden border-white/5">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/5 text-left text-[var(--text-muted)] bg-white/[0.02]">
                  <th className="px-8 py-5 font-black uppercase tracking-widest text-[10px]">Customer</th>
                  <th className="px-8 py-5 font-black uppercase tracking-widest text-[10px]">Product / Item</th>
                  <th className="px-8 py-5 font-black uppercase tracking-widest text-[10px] text-right">Revenue</th>
                  <th className="px-8 py-5 font-black uppercase tracking-widest text-[10px]">Purchased Date</th>
                  <th className="px-8 py-5 font-black uppercase tracking-widest text-[10px]">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {!stats || stats.recentPurchases.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-8 py-20 text-center text-[var(--text-secondary)] font-medium">No recent purchases recorded in the treasury.</td>
                  </tr>
                ) : (
                  stats.recentPurchases.map((p) => {
                    const customerName = p.userId?.minecraftUsername || p.userId?.discordUsername || "Unknown User";
                    return (
                      <tr key={p._id} className="hover:bg-white/[0.02] transition-colors group">
                        <td className="px-8 py-5">
                          <div className="font-bold text-white group-hover:text-[var(--accent)] transition-colors">{customerName}</div>
                          <div className="text-[10px] text-[var(--text-muted)] uppercase tracking-tighter">{p.userId?.discordId || "External"}</div>
                        </td>
                        <td className="px-8 py-5">
                          <span className="text-[var(--text-secondary)] font-medium">{p.itemName}</span>
                        </td>
                        <td className="px-8 py-5 text-right">
                          <span className="text-emerald-400 font-black tracking-tight text-lg">${(p.price / 100).toFixed(2)}</span>
                        </td>
                        <td className="px-8 py-5 text-[var(--text-muted)]">
                          {new Date(p.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                        </td>
                        <td className="px-8 py-5">
                          <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest ${
                            p.status === 'delivered' ? 'bg-emerald-400/10 text-emerald-400' : 'bg-amber-400/10 text-amber-400 animate-pulse'
                          }`}>
                            <span className={`w-1.5 h-1.5 rounded-full ${p.status === 'delivered' ? 'bg-emerald-500' : 'bg-amber-500'}`} />
                            {p.status}
                          </span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
