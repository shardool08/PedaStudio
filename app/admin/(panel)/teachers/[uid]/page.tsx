"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { PageHeader, StatCard, TierBadge } from "../../../components/AdminShell";

interface TeacherDetail {
  uid: string;
  teacherName: string;
  phoneNumber: string;
  district: string;
  schoolName: string;
  medium: string;
  tier: string;
  tierExpiresAt: string | null;
  profileComplete: boolean;
  usage: { month: string; plans: number; worksheets: number; scans: number; ocrScans: number };
  planCount: number;
  assessmentCount: number;
  subscription: { status: string; planId: string | null; expiresAt: string | null };
  profile: Record<string, unknown>;
  recentPlans: Array<{ id: string; lessonId: string; day: number; status: string }>;
  recentAssessments: Array<{ id: string; type: string; grade: number; scorePercent: number }>;
  payments: Array<{ id: string; tier: string; amountPaise: number; createdAt: string | null }>;
}

export default function TeacherDetailPage() {
  const { uid } = useParams<{ uid: string }>();
  const router = useRouter();
  const [teacher, setTeacher] = useState<TeacherDetail | null>(null);
  const [error, setError] = useState("");
  const [tier, setTier] = useState("basic");
  const [days, setDays] = useState("90");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  function load() {
    fetch(`/api/admin/teachers/${uid}`)
      .then((r) => r.json())
      .then((data) => {
        if (data.error) setError(data.error);
        else {
          setTeacher(data);
          setTier(data.tier);
        }
      })
      .catch(() => setError("Could not load teacher"));
  }

  useEffect(load, [uid]);

  async function updateTier() {
    setSaving(true);
    setMessage("");
    const body: Record<string, unknown> = { tier };
    if (tier !== "basic" && days) body.days = Number(days);
    const res = await fetch(`/api/admin/teachers/${uid}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const data = await res.json();
    setSaving(false);
    if (!res.ok) {
      setMessage(data.error || "Update failed");
      return;
    }
    setTeacher(data.teacher);
    setMessage("Tier updated");
  }

  async function resetUsage() {
    if (!confirm("Reset this month's usage counters to zero?")) return;
    setSaving(true);
    const res = await fetch(`/api/admin/teachers/${uid}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ action: "resetUsage" }),
    });
    const data = await res.json();
    setSaving(false);
    if (res.ok) {
      setTeacher(data.teacher);
      setMessage("Usage reset");
    }
  }

  if (error) {
    return (
      <>
        <PageHeader title="Teacher not found" />
        <p className="text-sm text-red-600">{error}</p>
        <Link href="/admin/teachers" className="mt-4 inline-block text-sm text-[#2A7A6A]">
          ← Back to teachers
        </Link>
      </>
    );
  }

  if (!teacher) {
    return <p className="text-sm text-[#496580]/60">Loading…</p>;
  }

  return (
    <>
      <div className="mb-4">
        <button
          type="button"
          onClick={() => router.push("/admin/teachers")}
          className="text-sm text-[#2A7A6A] hover:underline"
        >
          ← Teachers
        </button>
      </div>

      <PageHeader
        title={teacher.teacherName}
        description={`${teacher.phoneNumber} · ${teacher.district} · ${teacher.schoolName}`}
      />

      {message && (
        <div className="mb-4 rounded-lg bg-[#F0FAF8] px-4 py-2 text-sm text-[#2A7A6A]">
          {message}
        </div>
      )}

      <div className="mb-6 flex flex-wrap items-center gap-3">
        <TierBadge tier={teacher.tier} />
        {teacher.tierExpiresAt && (
          <span className="text-xs text-[#496580]/60">
            Expires {new Date(teacher.tierExpiresAt).toLocaleDateString()}
          </span>
        )}
        <span className="text-xs text-[#496580]/60">
          Subscription: {teacher.subscription.status}
        </span>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Plans saved" value={teacher.planCount} />
        <StatCard label="Assessments" value={teacher.assessmentCount} />
        <StatCard label="Plans this month" value={teacher.usage.plans} />
        <StatCard label="Scans this month" value={teacher.usage.scans} />
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-[#D0EAE4] bg-white p-5">
          <h3 className="text-sm font-semibold text-[#496580]">Manage tier</h3>
          <div className="mt-4 space-y-3">
            <select
              value={tier}
              onChange={(e) => setTier(e.target.value)}
              className="w-full rounded-lg border border-[#D0EAE4] px-3 py-2 text-sm"
            >
              <option value="basic">Basic</option>
              <option value="prime">Prime</option>
              <option value="max">Max</option>
            </select>
            {tier !== "basic" && (
              <input
                type="number"
                value={days}
                onChange={(e) => setDays(e.target.value)}
                placeholder="Days valid"
                className="w-full rounded-lg border border-[#D0EAE4] px-3 py-2 text-sm"
              />
            )}
            <div className="flex gap-2">
              <button
                type="button"
                onClick={updateTier}
                disabled={saving}
                className="rounded-lg bg-[#2A7A6A] px-4 py-2 text-sm font-semibold text-white hover:bg-[#3A9A8A] disabled:opacity-50"
              >
                Save tier
              </button>
              <button
                type="button"
                onClick={resetUsage}
                disabled={saving}
                className="rounded-lg border border-[#D0EAE4] px-4 py-2 text-sm text-[#496580] hover:bg-[#F0FAF8] disabled:opacity-50"
              >
                Reset usage
              </button>
            </div>
          </div>
        </section>

        <section className="rounded-xl border border-[#D0EAE4] bg-white p-5">
          <h3 className="text-sm font-semibold text-[#496580]">Profile</h3>
          <dl className="mt-3 space-y-2 text-sm">
            {[
              ["Medium", teacher.medium],
              ["Language", String(teacher.profile.language || "—")],
              ["Grades", Array.isArray(teacher.profile.teacherGrades) ? (teacher.profile.teacherGrades as number[]).join(", ") : "—"],
              ["Students", String(teacher.profile.studentCount || "—")],
              ["Profile complete", teacher.profileComplete ? "Yes" : "No"],
            ].map(([k, v]) => (
              <div key={k} className="flex justify-between gap-4">
                <dt className="text-[#496580]/60">{k}</dt>
                <dd className="text-right text-[#496580]">{v}</dd>
              </div>
            ))}
          </dl>
          <p className="mt-3 text-xs text-[#496580]/40">UID: {teacher.uid}</p>
        </section>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-[#D0EAE4] bg-white p-5">
          <h3 className="text-sm font-semibold text-[#496580]">Recent plans</h3>
          {teacher.recentPlans.length === 0 ? (
            <p className="mt-2 text-sm text-[#496580]/50">No plans yet</p>
          ) : (
            <ul className="mt-2 space-y-1 text-sm">
              {teacher.recentPlans.map((p) => (
                <li key={p.id} className="flex justify-between text-[#496580]/80">
                  <span>
                    Lesson {p.lessonId} day {p.day}
                  </span>
                  <span className="text-xs capitalize text-[#496580]/50">{p.status}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-xl border border-[#D0EAE4] bg-white p-5">
          <h3 className="text-sm font-semibold text-[#496580]">Recent assessments</h3>
          {teacher.recentAssessments.length === 0 ? (
            <p className="mt-2 text-sm text-[#496580]/50">No assessments yet</p>
          ) : (
            <ul className="mt-2 space-y-1 text-sm">
              {teacher.recentAssessments.map((a) => (
                <li key={a.id} className="flex justify-between text-[#496580]/80">
                  <span className="capitalize">
                    {a.type} · Grade {a.grade}
                  </span>
                  <span>{a.scorePercent}%</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {teacher.payments.length > 0 && (
        <section className="mt-6 rounded-xl border border-[#D0EAE4] bg-white p-5">
          <h3 className="text-sm font-semibold text-[#496580]">Payments</h3>
          <ul className="mt-2 space-y-1 text-sm">
            {teacher.payments.map((p) => (
              <li key={p.id} className="flex justify-between text-[#496580]/80">
                <span className="capitalize">{p.tier}</span>
                <span>₹{(p.amountPaise / 100).toFixed(0)}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}
