import { NextRequest, NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import { listTeachers } from "@/lib/admin-service";
import type { TierId } from "@/lib/tier-config";

export async function GET(req: NextRequest) {
  const denied = await requireAdmin();
  if (denied) return denied;

  const { searchParams } = req.nextUrl;
  const limit = Number(searchParams.get("limit")) || 50;
  const cursor = searchParams.get("cursor") || undefined;
  const search = searchParams.get("search") || undefined;
  const tier = (searchParams.get("tier") as TierId) || undefined;

  try {
    const result = await listTeachers({ limit, cursor, search, tier });
    return NextResponse.json(result);
  } catch (err) {
    console.error("ADMIN TEACHERS:", err);
    return NextResponse.json({ error: "Could not list teachers" }, { status: 500 });
  }
}
