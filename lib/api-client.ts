import { getSupabaseBrowser, isSupabaseBrowserConfigured } from "@/lib/supabase/browser";

/**
 * `fetch` for `/api/*` routes, with the teacher's access token attached.
 *
 * Mirrors what the Android app sends, so both clients hit the same route guards.
 */
export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);

  if (isSupabaseBrowserConfigured()) {
    // getSession() renews the token first if it has expired.
    const { data } = await getSupabaseBrowser().auth.getSession();
    const token = data.session?.access_token;
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  return fetch(path, { ...init, headers });
}

/** As `apiFetch`, but parses JSON and turns an error status into a thrown Error. */
export async function apiJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await apiFetch(path, init);
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error((body as { error?: string }).error || `Request failed (${res.status})`);
  }
  return body as T;
}
