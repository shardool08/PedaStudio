"use client";

import { useEffect, useState } from "react";
import { PageHeader, StatCard } from "../components/AdminShell";

interface DashboardStats {
  totalTeachers: number;
  profileComplete: number;
  tierCounts: { basic: number; prime: number; max: number };
  usageThisMonth: {
    plans: number;
    worksheets: number;
    scans: number;
    ocrScans: number;
  };
  catalog: { tlmResources: number; flashcardLessons: number };
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch("/api/admin/stats")
      .then((r) => r.json())
      .then((data) => {
        if (data.error) setError(data.error);
        else setStats(data);
      })
      .catch(() => setError("Could not load dashboard"));
  }, []);

  return (
    <>
      <PageHeader
        title="Dashboard"
        description="Overview of teachers, usage, and catalog."
      />

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {!stats && !error && (
        <p className="text-sm text-[#496580]/60">Loading…</p>
      )}

      {stats && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Total teachers" value={stats.totalTeachers} />
            <StatCard
              label="Profiles complete"
              value={stats.profileComplete}
              sub={`${Math.round((stats.profileComplete / Math.max(stats.totalTeachers, 1)) * 100)}% of registered`}
            />
            <StatCard
              label="TLM resources"
              value={stats.catalog.tlmResources}
            />
            <StatCard
              label="Flashcard lessons"
              value={stats.catalog.flashcardLessons}
            />
          </div>

          <h3 className="mb-3 mt-8 text-sm font-semibold uppercase tracking-wide text-[#496580]/60">
            Subscription tiers
          </h3>
          <div className="grid gap-4 sm:grid-cols-3">
            <StatCard label="Basic" value={stats.tierCounts.basic} />
            <StatCard label="Prime" value={stats.tierCounts.prime} />
            <StatCard label="Max" value={stats.tierCounts.max} />
          </div>

          <h3 className="mb-3 mt-8 text-sm font-semibold uppercase tracking-wide text-[#496580]/60">
            Usage this month (all teachers)
          </h3>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Plans generated" value={stats.usageThisMonth.plans} />
            <StatCard label="Worksheets" value={stats.usageThisMonth.worksheets} />
            <StatCard label="Textbook scans" value={stats.usageThisMonth.scans} />
            <StatCard label="OCR scans" value={stats.usageThisMonth.ocrScans} />
          </div>
        </>
      )}
    </>
  );
}
