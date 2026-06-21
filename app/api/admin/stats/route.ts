import { NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import { getDashboardStats } from "@/lib/admin-service";

export async function GET() {
  const denied = await requireAdmin();
  if (denied) return denied;
  try {
    const stats = await getDashboardStats();
    return NextResponse.json(stats);
  } catch (err) {
    console.error("ADMIN STATS:", err);
    return NextResponse.json({ error: "Could not load stats" }, { status: 500 });
  }
}
