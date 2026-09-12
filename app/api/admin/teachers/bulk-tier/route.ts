import { NextRequest, NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import { getDashboardStats, setAllTeachersTier } from "@/lib/admin-service";
import type { TierId } from "@/lib/tier-config";

export async function POST(req: NextRequest) {
  const denied = await requireAdmin(req);
  if (denied) return denied;

  const body = await req.json().catch(() => ({}));
  const tier = body.tier as TierId;
  if (!["basic", "prime", "max"].includes(tier)) {
    return NextResponse.json({ error: "Invalid tier" }, { status: 400 });
  }

  const days = body.days ? Number(body.days) : undefined;

  try {
    const updated = await setAllTeachersTier(tier, days);
    const stats = await getDashboardStats();
    return NextResponse.json({ ok: true, updated, stats });
  } catch (err) {
    console.error("ADMIN BULK TIER:", err);
    return NextResponse.json({ error: "Bulk tier update failed" }, { status: 500 });
  }
}
