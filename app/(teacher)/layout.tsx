import { AuthProvider } from "@/lib/auth-context";

/** Wraps every teacher-facing page in the Supabase session context. */
export default function TeacherLayout({ children }: { children: React.ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}
