import { NextRequest, NextResponse } from "next/server";
import { COOKIE_NAME, parseAdminSessionTokenEdge } from "@/lib/admin-auth-edge";

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;

  if (!pathname.startsWith("/admin")) return NextResponse.next();
  if (pathname === "/admin/login") return NextResponse.next();

  const secret = process.env.ADMIN_SECRET?.trim();
  if (!secret) {
    const url = req.nextUrl.clone();
    url.pathname = "/admin/login";
    url.searchParams.set("error", "not-configured");
    return NextResponse.redirect(url);
  }

  const token = req.cookies.get(COOKIE_NAME)?.value;
  const ok = await parseAdminSessionTokenEdge(token, secret);
  if (!ok) {
    const url = req.nextUrl.clone();
    url.pathname = "/admin/login";
    url.searchParams.set("next", pathname);
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
