import { NextRequest, NextResponse } from "next/server";
import { requireApiUser } from "@/lib/api-auth";
import { getAccountWithSubscription } from "@/lib/subscription-service";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const auth = await requireApiUser(req);
  if (auth instanceof NextResponse) return auth;

  const account = await getAccountWithSubscription(auth.uid);
  return NextResponse.json(account);
}
