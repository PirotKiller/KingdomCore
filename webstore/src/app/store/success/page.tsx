import Link from "next/link";

export default function SuccessPage() {
  return (
    <div className="max-w-xl mx-auto px-4 py-24 text-center">
      <div className="glass-card p-10">
        <div className="text-6xl mb-6">✅</div>
        <h1 className="text-3xl font-bold text-white mb-4">Purchase Successful!</h1>
        <p className="text-[var(--text-secondary)] mb-2">
          Your order has been processed successfully.
        </p>
        <p className="text-[var(--text-secondary)] mb-8">
          Currency purchases are delivered instantly. Items will be delivered when you next join the server.
        </p>
        <Link
          href="/store"
          className="inline-block px-6 py-3 rounded-xl bg-gradient-to-r from-[var(--accent)] to-purple-600 text-white font-semibold hover:shadow-lg transition-all"
        >
          Back to Store
        </Link>
      </div>
    </div>
  );
}
