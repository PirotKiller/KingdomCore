"use client";

interface PaginationProps {
  page: number;
  total: number;
  totalPages: number;
  limit: number;
  onPageChange: (newPage: number) => void;
  loading?: boolean;
}

export default function Pagination({
  page,
  total,
  totalPages,
  limit,
  onPageChange,
  loading = false,
}: PaginationProps) {
  if (totalPages <= 1 && total === 0) return null;

  const start = (page - 1) * limit + 1;
  const end = Math.min(page * limit, total);

  return (
    <div className="p-4 border-t border-[var(--border)] flex flex-col sm:flex-row items-center justify-between gap-4 bg-white/[0.02]">
      <div className="text-xs text-[var(--text-muted)] flex items-center gap-1.5 order-2 sm:order-1">
        Showing <span className="text-white font-bold tabular-nums">{total === 0 ? 0 : start}</span> 
        to <span className="text-white font-bold tabular-nums">{end}</span> 
        of <span className="text-white font-bold tabular-nums">{total}</span> results
      </div>
      
      <div className="flex items-center gap-2 order-1 sm:order-2">
        <button
          onClick={() => onPageChange(Math.max(1, page - 1))}
          disabled={page === 1 || loading}
          className="p-2 px-4 rounded-xl bg-white/5 border border-white/10 text-[var(--text-muted)] hover:text-white hover:bg-white/10 disabled:opacity-30 disabled:hover:bg-white/5 transition-all font-black text-[10px] uppercase tracking-[0.2em] flex items-center gap-2 group"
        >
          <svg className="w-3 h-3 group-hover:-translate-x-0.5 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M15 19l-7-7 7-7" />
          </svg>
          Prev
        </button>

        {/* Page indicator for mobile/compact */}
        <div className="px-4 py-2 rounded-xl bg-[var(--accent)]/10 border border-[var(--accent)]/20 text-[var(--accent)] font-black text-xs min-w-[3rem] text-center">
          {page}
        </div>

        <button
          onClick={() => onPageChange(Math.min(totalPages, page + 1))}
          disabled={page === totalPages || loading}
          className="p-2 px-4 rounded-xl bg-white/5 border border-white/10 text-[var(--text-muted)] hover:text-white hover:bg-white/10 disabled:opacity-30 disabled:hover:bg-white/5 transition-all font-black text-[10px] uppercase tracking-[0.2em] flex items-center gap-2 group"
        >
          Next
          <svg className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>
    </div>
  );
}
