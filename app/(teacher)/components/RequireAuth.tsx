"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth-context";

/**
 * Client-side guard for teacher pages. Redirects to `/login` when there is no session.
 *
 * The API routes are the real security boundary — every `/api/*` handler verifies the
 * token itself, so this only decides what the browser bothers to render.
 */
export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { session, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (loading || session) return;
    router.replace(`/login?next=${encodeURIComponent(pathname)}`);
  }, [loading, session, pathname, router]);

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#F8FCFB]">
        <p className="text-sm text-[#496580]/60">Loading…</p>
      </main>
    );
  }

  if (!session) return null;

  return <>{children}</>;
}
