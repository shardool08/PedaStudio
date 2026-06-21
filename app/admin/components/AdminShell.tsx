"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";

const NAV = [
  { href: "/admin", label: "Dashboard", exact: true },
  { href: "/admin/teachers", label: "Teachers" },
  { href: "/admin/catalog", label: "Catalog" },
];

export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  async function logout() {
    await fetch("/api/admin/login", { method: "DELETE" });
    router.push("/admin/login");
    router.refresh();
  }

  return (
    <div className="flex min-h-screen bg-[#F8FCFB]">
      <aside className="flex w-56 shrink-0 flex-col border-r border-[#D0EAE4] bg-white">
        <div className="border-b border-[#D0EAE4] px-5 py-4">
          <p className="text-xs font-medium uppercase tracking-wide text-[#496580]/60">
            PedaStudio
          </p>
          <h1 className="text-lg font-bold text-[#496580]">Admin</h1>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {NAV.map((item) => {
            const active = item.exact
              ? pathname === item.href
              : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  active
                    ? "bg-[#2A7A6A] text-white"
                    : "text-[#496580] hover:bg-[#F0FAF8]"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="border-t border-[#D0EAE4] p-3">
          <button
            type="button"
            onClick={logout}
            className="w-full rounded-lg px-3 py-2 text-left text-sm text-[#496580]/70 hover:bg-[#F0FAF8]"
          >
            Sign out
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-auto p-6 md:p-8">{children}</main>
    </div>
  );
}

export function StatCard({
  label,
  value,
  sub,
}: {
  label: string;
  value: string | number;
  sub?: string;
}) {
  return (
    <div className="rounded-xl border border-[#D0EAE4] bg-white p-5 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wide text-[#496580]/60">
        {label}
      </p>
      <p className="mt-1 text-3xl font-bold text-[#496580]">{value}</p>
      {sub && <p className="mt-1 text-xs text-[#496580]/50">{sub}</p>}
    </div>
  );
}

export function TierBadge({ tier }: { tier: string }) {
  const colors: Record<string, string> = {
    basic: "bg-[#F0FAF8] text-[#496580]",
    prime: "bg-[#FFDBBB] text-[#7A4A1A]",
    max: "bg-[#2A7A6A] text-white",
  };
  return (
    <span
      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold capitalize ${colors[tier] || colors.basic}`}
    >
      {tier}
    </span>
  );
}

export function PageHeader({
  title,
  description,
}: {
  title: string;
  description?: string;
}) {
  return (
    <div className="mb-6">
      <h2 className="text-2xl font-bold text-[#496580]">{title}</h2>
      {description && (
        <p className="mt-1 text-sm text-[#496580]/60">{description}</p>
      )}
    </div>
  );
}
