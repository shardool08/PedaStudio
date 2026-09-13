"use client";

import type { Session } from "@supabase/supabase-js";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { getSupabaseBrowser, isSupabaseBrowserConfigured } from "@/lib/supabase/browser";

interface AuthValue {
  session: Session | null;
  uid: string | null;
  phone: string | null;
  /** True until the stored session has been read; guards must wait it out. */
  loading: boolean;
  /** False when the Supabase env vars are missing, so pages can say so instead of crashing. */
  configured: boolean;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const configured = isSupabaseBrowserConfigured();

  useEffect(() => {
    if (!configured) {
      setLoading(false);
      return;
    }

    const supabase = getSupabaseBrowser();
    let active = true;

    supabase.auth.getSession().then(({ data }) => {
      if (!active) return;
      setSession(data.session);
      setLoading(false);
    });

    const { data } = supabase.auth.onAuthStateChange((_event, next) => {
      if (!active) return;
      setSession(next);
      setLoading(false);
    });

    return () => {
      active = false;
      data.subscription.unsubscribe();
    };
  }, [configured]);

  const value = useMemo<AuthValue>(
    () => ({
      session,
      uid: session?.user.id ?? null,
      phone: session?.user.phone ?? null,
      loading,
      configured,
      signOut: async () => {
        if (configured) await getSupabaseBrowser().auth.signOut();
        setSession(null);
      },
    }),
    [session, loading, configured],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside <AuthProvider>");
  return value;
}
