"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { PageHeader, TierBadge } from "../../components/AdminShell";

interface TeacherRow {
  uid: string;
  teacherName: string;
  phoneNumber: string;
  district: string;
  schoolName: string;
  medium: string;
  tier: string;
  profileComplete: boolean;
  usage: { plans: number; worksheets: number; scans: number };
  planCount: number;
  updatedAt: string | null;
}

export default function TeachersPage() {
  const [teachers, setTeachers] = useState<TeacherRow[]>([]);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState("");
  const [tierFilter, setTierFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams();
    if (search) params.set("search", search);
    if (tierFilter) params.set("tier", tierFilter);
    fetch(`/api/admin/teachers?${params}`)
      .then((r) => r.json())
      .then((data) => {
        if (data.error) setError(data.error);
        else {
          setTeachers(data.teachers);
          setTotal(data.total);
        }
      })
      .catch(() => setError("Could not load teachers"))
      .finally(() => setLoading(false));
  }, [search, tierFilter]);

  return (
    <>
      <PageHeader
        title="Teachers"
        description={`${total} registered teachers`}
      />

      <div className="mb-4 flex flex-wrap gap-3">
        <input
          type="search"
          placeholder="Search name, phone, district…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="rounded-lg border border-[#D0EAE4] px-3 py-2 text-sm text-[#496580] outline-none focus:border-[#2A7A6A] md:w-72"
        />
        <select
          value={tierFilter}
          onChange={(e) => setTierFilter(e.target.value)}
          className="rounded-lg border border-[#D0EAE4] px-3 py-2 text-sm text-[#496580] outline-none focus:border-[#2A7A6A]"
        >
          <option value="">All tiers</option>
          <option value="basic">Basic</option>
          <option value="prime">Prime</option>
          <option value="max">Max</option>
        </select>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-xl border border-[#D0EAE4] bg-white">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-[#D0EAE4] bg-[#F0FAF8]">
            <tr>
              <th className="px-4 py-3 font-semibold text-[#496580]">Teacher</th>
              <th className="hidden px-4 py-3 font-semibold text-[#496580] md:table-cell">
                District
              </th>
              <th className="px-4 py-3 font-semibold text-[#496580]">Tier</th>
              <th className="hidden px-4 py-3 font-semibold text-[#496580] sm:table-cell">
                Plans
              </th>
              <th className="hidden px-4 py-3 font-semibold text-[#496580] lg:table-cell">
                Usage (mo)
              </th>
              <th className="px-4 py-3 font-semibold text-[#496580]"></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[#496580]/50">
                  Loading…
                </td>
              </tr>
            )}
            {!loading && teachers.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[#496580]/50">
                  No teachers found
                </td>
              </tr>
            )}
            {teachers.map((t) => (
              <tr
                key={t.uid}
                className="border-b border-[#D0EAE4]/50 hover:bg-[#F8FCFB]"
              >
                <td className="px-4 py-3">
                  <p className="font-medium text-[#496580]">{t.teacherName}</p>
                  <p className="text-xs text-[#496580]/50">{t.phoneNumber}</p>
                </td>
                <td className="hidden px-4 py-3 text-[#496580]/70 md:table-cell">
                  {t.district}
                </td>
                <td className="px-4 py-3">
                  <TierBadge tier={t.tier} />
                </td>
                <td className="hidden px-4 py-3 text-[#496580]/70 sm:table-cell">
                  {t.planCount}
                </td>
                <td className="hidden px-4 py-3 text-xs text-[#496580]/60 lg:table-cell">
                  {t.usage.plans}p · {t.usage.worksheets}w · {t.usage.scans}s
                </td>
                <td className="px-4 py-3 text-right">
                  <Link
                    href={`/admin/teachers/${t.uid}`}
                    className="text-sm font-medium text-[#2A7A6A] hover:underline"
                  >
                    View
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
