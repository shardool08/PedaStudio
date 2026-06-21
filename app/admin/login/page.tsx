"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const notConfigured = params.get("error") === "not-configured";
  const next = params.get("next") || "/admin";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError("");
    const res = await fetch("/api/admin/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password }),
    });
    setLoading(false);
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      setError(data.error || "Login failed");
      return;
    }
    router.push(next);
    router.refresh();
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#F8FCFB] px-4">
      <div className="w-full max-w-sm rounded-2xl border border-[#D0EAE4] bg-white p-8 shadow-sm">
        <p className="text-xs font-medium uppercase tracking-wide text-[#496580]/60">
          PedaStudio
        </p>
        <h1 className="mt-1 text-2xl font-bold text-[#496580]">Admin Panel</h1>
        <p className="mt-2 text-sm text-[#496580]/60">
          Manage teachers, tiers, and catalog data.
        </p>

        {notConfigured && (
          <div className="mt-4 rounded-lg bg-[#FFDBBB]/50 px-3 py-2 text-sm text-[#7A4A1A]">
            Set <code className="text-xs">ADMIN_SECRET</code> in your environment to
            enable the admin panel.
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label
              htmlFor="password"
              className="block text-sm font-medium text-[#496580]"
            >
              Admin password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded-lg border border-[#D0EAE4] px-3 py-2 text-sm text-[#496580] outline-none focus:border-[#2A7A6A] focus:ring-1 focus:ring-[#2A7A6A]"
              autoFocus
              required
            />
          </div>
          {error && (
            <p className="text-sm text-red-600">{error}</p>
          )}
          <button
            type="submit"
            disabled={loading || notConfigured}
            className="w-full rounded-lg bg-[#2A7A6A] py-2.5 text-sm font-semibold text-white transition hover:bg-[#3A9A8A] disabled:opacity-50"
          >
            {loading ? "Signing in…" : "Sign in"}
          </button>
        </form>
      </div>
    </main>
  );
}

export default function AdminLoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}
