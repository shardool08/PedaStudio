"use client";

import { useEffect, useState } from "react";
import { PageHeader, StatCard } from "../../components/AdminShell";

interface CatalogData {
  tlm: { exists: boolean; count: number; updatedAt: string | null };
  flashcards: {
    lessonCount: number;
    lessons: Array<{
      id: string;
      lessonId: string;
      title: string;
      cardCount: number;
      updatedAt: string | null;
    }>;
  };
}

export default function CatalogPage() {
  const [catalog, setCatalog] = useState<CatalogData | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch("/api/admin/catalog")
      .then((r) => r.json())
      .then((data) => {
        if (data.error) setError(data.error);
        else setCatalog(data);
      })
      .catch(() => setError("Could not load catalog"));
  }, []);

  return (
    <>
      <PageHeader
        title="Catalog"
        description="Supabase catalog for TLM resources and flashcards."
      />

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {!catalog && !error && (
        <p className="text-sm text-[#496580]/60">Loading…</p>
      )}

      {catalog && (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            <StatCard
              label="TLM resources"
              value={catalog.tlm.count}
              sub={
                catalog.tlm.updatedAt
                  ? `Updated ${new Date(catalog.tlm.updatedAt).toLocaleDateString()}`
                  : catalog.tlm.exists
                    ? "Seeded"
                    : "Not seeded — run npm run supabase:seed-catalog"
              }
            />
            <StatCard
              label="Flashcard lessons"
              value={catalog.flashcards.lessonCount}
            />
          </div>

          <div className="mt-6 rounded-xl border border-[#D0EAE4] bg-[#F0FAF8] p-5">
            <h3 className="text-sm font-semibold text-[#496580]">Seed catalog</h3>
            <p className="mt-2 text-sm text-[#496580]/70">
              Upload TLM and flashcard data from bundled Android assets to Supabase:
            </p>
            <code className="mt-2 block rounded-lg bg-white px-3 py-2 text-xs text-[#496580]">
              npm run supabase:seed-catalog
            </code>
            <p className="mt-3 text-xs text-[#496580]/50">
              Requires NEXT_PUBLIC_SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY.
            </p>
          </div>

          {catalog.flashcards.lessons.length > 0 && (
            <div className="mt-6 overflow-hidden rounded-xl border border-[#D0EAE4] bg-white">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-[#D0EAE4] bg-[#F0FAF8]">
                  <tr>
                    <th className="px-4 py-3 font-semibold text-[#496580]">Lesson</th>
                    <th className="px-4 py-3 font-semibold text-[#496580]">Title</th>
                    <th className="px-4 py-3 font-semibold text-[#496580]">Cards</th>
                  </tr>
                </thead>
                <tbody>
                  {catalog.flashcards.lessons.map((l) => (
                    <tr
                      key={l.id}
                      className="border-b border-[#D0EAE4]/50"
                    >
                      <td className="px-4 py-3 font-mono text-xs text-[#496580]">
                        {l.lessonId}
                      </td>
                      <td className="px-4 py-3 text-[#496580]/80">{l.title}</td>
                      <td className="px-4 py-3 text-[#496580]/60">{l.cardCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  );
}
