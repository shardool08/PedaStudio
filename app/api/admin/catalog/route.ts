import { NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import { getCatalogStatus } from "@/lib/admin-service";

export async function GET() {
  const denied = await requireAdmin();
  if (denied) return denied;
  try {
    const catalog = await getCatalogStatus();
    return NextResponse.json(catalog);
  } catch (err) {
    console.error("ADMIN CATALOG:", err);
    return NextResponse.json({ error: "Could not load catalog" }, { status: 500 });
  }
}
