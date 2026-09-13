"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { useAuth } from "@/lib/auth-context";
import { getSupabaseBrowser, toE164 } from "@/lib/supabase/browser";

const OTP_LENGTH = 6;

/** Supabase auth errors are developer-facing; teachers get plain language instead. */
function friendlyError(message: string): string {
  const text = message.toLowerCase();
  if (text.includes("rate limit") || text.includes("too many")) {
    return "Too many attempts. Please wait a few minutes and try again.";
  }
  if (text.includes("expired") || text.includes("invalid token") || text.includes("otp")) {
    return "That OTP is wrong or has expired. Ask for a new one.";
  }
  if (text.includes("phone") && text.includes("invalid")) {
    return "That mobile number is not valid.";
  }
  if (text.includes("provider") || text.includes("not enabled") || text.includes("unsupported")) {
    return "Phone sign-in is not switched on for this server yet.";
  }
  return message || "Could not sign in. Please try again.";
}

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const { configured } = useAuth();

  const [step, setStep] = useState<"phone" | "otp">("phone");
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const next = params.get("next") || "/";

  async function sendOtp(e?: React.FormEvent) {
    e?.preventDefault();
    if (phone.length !== 10) {
      setError("Enter your 10-digit mobile number.");
      return;
    }
    setLoading(true);
    setError("");
    const { error: sendError } = await getSupabaseBrowser().auth.signInWithOtp({
      phone: toE164(phone),
    });
    setLoading(false);
    if (sendError) {
      setError(friendlyError(sendError.message));
      return;
    }
    setOtp("");
    setStep("otp");
  }

  async function verifyOtp(e: React.FormEvent) {
    e.preventDefault();
    if (otp.length !== OTP_LENGTH) {
      setError(`Enter the ${OTP_LENGTH}-digit OTP.`);
      return;
    }
    setLoading(true);
    setError("");
    const { error: verifyError } = await getSupabaseBrowser().auth.verifyOtp({
      phone: toE164(phone),
      token: otp,
      type: "sms",
    });
    setLoading(false);
    if (verifyError) {
      setError(friendlyError(verifyError.message));
      return;
    }
    router.replace(next);
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#F8FCFB] px-4">
      <div className="w-full max-w-sm rounded-2xl border border-[#D0EAE4] bg-white p-8 shadow-sm">
        <p className="text-xs font-medium uppercase tracking-wide text-[#496580]/60">
          PedaStudio
        </p>
        <h1 className="mt-1 text-2xl font-bold text-[#496580]">Teacher sign in</h1>

        {!configured ? (
          <div className="mt-6 rounded-lg bg-[#FFDBBB]/50 px-3 py-2 text-sm text-[#7A4A1A]">
            Set <code className="text-xs">NEXT_PUBLIC_SUPABASE_URL</code> and{" "}
            <code className="text-xs">NEXT_PUBLIC_SUPABASE_ANON_KEY</code> to enable
            sign-in.
          </div>
        ) : step === "phone" ? (
          <form onSubmit={sendOtp} className="mt-6 space-y-4">
            <p className="text-sm text-[#496580]/60">
              We will send a one-time password to your mobile number.
            </p>
            <div>
              <label htmlFor="phone" className="block text-sm font-medium text-[#496580]">
                Mobile number
              </label>
              <div className="mt-1 flex items-center gap-2">
                <span className="rounded-lg border border-[#D0EAE4] bg-[#F0FAF8] px-3 py-2.5 text-sm text-[#496580]">
                  +91
                </span>
                <input
                  id="phone"
                  type="tel"
                  inputMode="numeric"
                  autoComplete="tel-national"
                  value={phone}
                  onChange={(e) => {
                    setPhone(e.target.value.replace(/\D/g, "").slice(0, 10));
                    setError("");
                  }}
                  className="w-full rounded-lg border border-[#D0EAE4] px-3 py-2.5 text-lg tracking-wider text-[#496580] outline-none focus:border-[#2A7A6A] focus:ring-1 focus:ring-[#2A7A6A]"
                  placeholder="9876543210"
                  autoFocus
                  required
                />
              </div>
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <button
              type="submit"
              disabled={loading || phone.length !== 10}
              className="w-full rounded-lg bg-[#2A7A6A] py-3 text-sm font-semibold text-white transition hover:bg-[#3A9A8A] disabled:opacity-50"
            >
              {loading ? "Sending OTP…" : "Send OTP"}
            </button>
          </form>
        ) : (
          <form onSubmit={verifyOtp} className="mt-6 space-y-4">
            <p className="text-sm text-[#496580]/60">
              Enter the OTP sent to <span className="font-medium">+91 {phone}</span>.
            </p>
            <div>
              <label htmlFor="otp" className="block text-sm font-medium text-[#496580]">
                OTP
              </label>
              <input
                id="otp"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={otp}
                onChange={(e) => {
                  setOtp(e.target.value.replace(/\D/g, "").slice(0, OTP_LENGTH));
                  setError("");
                }}
                className="mt-1 w-full rounded-lg border border-[#D0EAE4] px-3 py-2.5 text-center text-2xl tracking-[0.4em] text-[#496580] outline-none focus:border-[#2A7A6A] focus:ring-1 focus:ring-[#2A7A6A]"
                placeholder="······"
                autoFocus
                required
              />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <button
              type="submit"
              disabled={loading || otp.length !== OTP_LENGTH}
              className="w-full rounded-lg bg-[#2A7A6A] py-3 text-sm font-semibold text-white transition hover:bg-[#3A9A8A] disabled:opacity-50"
            >
              {loading ? "Checking…" : "Sign in"}
            </button>
            <div className="flex items-center justify-between text-sm">
              <button
                type="button"
                onClick={() => {
                  setStep("phone");
                  setError("");
                }}
                className="text-[#496580]/70 underline"
              >
                Change number
              </button>
              <button
                type="button"
                onClick={() => sendOtp()}
                disabled={loading}
                className="font-medium text-[#2A7A6A] underline disabled:opacity-50"
              >
                Resend OTP
              </button>
            </div>
          </form>
        )}
      </div>
    </main>
  );
}

export default function TeacherLoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}
