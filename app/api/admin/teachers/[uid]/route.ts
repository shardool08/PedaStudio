import { NextRequest, NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import {
  getTeacherDetail,
  resetTeacherUsage,
  setTeacherTier,
} from "@/lib/admin-service";
import type { TierId } from "@/lib/tier-config";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ uid: string }> },
) {
  const denied = await requireAdmin();
  if (denied) return denied;

  const { uid } = await params;
  try {
    const teacher = await getTeacherDetail(uid);
    if (!teacher) {
      return NextResponse.json({ error: "Teacher not found" }, { status: 404 });
    }
    return NextResponse.json(teacher);
  } catch (err) {
    console.error("ADMIN TEACHER GET:", err);
    return NextResponse.json({ error: "Could not load teacher" }, { status: 500 });
  }
}

export async function PATCH(
  req: NextRequest,
  { params }: { params: Promise<{ uid: string }> },
) {
  const denied = await requireAdmin();
  if (denied) return denied;

  const { uid } = await params;
  const body = await req.json().catch(() => ({}));

  try {
    if (body.action === "resetUsage") {
      await resetTeacherUsage(uid);
      const teacher = await getTeacherDetail(uid);
      return NextResponse.json({ ok: true, teacher });
    }

    if (body.tier) {
      const tier = body.tier as TierId;
      if (!["basic", "prime", "max"].includes(tier)) {
        return NextResponse.json({ error: "Invalid tier" }, { status: 400 });
      }
      const days = body.days ? Number(body.days) : undefined;
      await setTeacherTier(uid, tier, days);
      const teacher = await getTeacherDetail(uid);
      return NextResponse.json({ ok: true, teacher });
    }

    return NextResponse.json({ error: "No valid action" }, { status: 400 });
  } catch (err) {
    console.error("ADMIN TEACHER PATCH:", err);
    return NextResponse.json({ error: "Update failed" }, { status: 500 });
  }
}
