-- PedaStudio → Supabase (Postgres)
-- Mapped from lib/schema/* and docs/DATABASE.md
-- Run in Supabase SQL Editor, or: npm run supabase:migrate (with DATABASE_URL set)

create extension if not exists "pgcrypto";

-- ─── Enums ───────────────────────────────────────────────
do $$ begin
  create type tier_id as enum ('basic', 'prime', 'max');
exception when duplicate_object then null;
end $$;

do $$ begin
  create type plan_status as enum ('not_started', 'planned', 'completed');
exception when duplicate_object then null;
end $$;

do $$ begin
  create type assessment_type as enum ('baseline', 'unit', 'endline');
exception when duplicate_object then null;
end $$;

do $$ begin
  create type billing_cycle as enum ('monthly', 'yearly');
exception when duplicate_object then null;
end $$;

do $$ begin
  create type subscription_status as enum ('none', 'active', 'expired', 'pending');
exception when duplicate_object then null;
end $$;

-- ─── Teachers (was users/{uid}) ─────────────────────────
create table if not exists public.teachers (
  id uuid primary key references auth.users (id) on delete cascade,

  teacher_name text not null default '',
  phone_number text not null default '',
  language text not null default 'mr',
  state text not null default '',
  district text not null default '',
  admin_type text not null default '',
  zp_name text not null default '',
  corp_name text not null default '',
  medium text not null default 'marathi',
  english_comfort text not null default '',
  school_name text not null default '',
  location text not null default '',
  pin_code text not null default '',
  student_count integer not null default 0,
  internet_access text not null default '',
  printing_access text not null default '',
  teacher_grades integer[] not null default '{}',
  teacher_subjects text[] not null default '{}',
  teacher_resources text[] not null default '{}',
  current_lessons jsonb not null default '{}'::jsonb,
  profile_complete boolean not null default false,

  tier tier_id not null default 'basic',
  tier_expires_at timestamptz,
  usage_week text not null default '',
  usage_plans integer not null default 0,
  usage_worksheets integer not null default 0,
  usage_scans integer not null default 0,
  usage_ocr_scans integer not null default 0,
  max_trial_used boolean not null default false,

  sub_status subscription_status not null default 'none',
  sub_plan_id text,
  sub_billing_cycle billing_cycle,
  sub_tier tier_id not null default 'basic',
  sub_started_at timestamptz,
  sub_expires_at timestamptz,
  razorpay_order_id text,
  razorpay_payment_id text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists teachers_phone_unique on public.teachers (phone_number)
  where phone_number <> '';

-- ─── Lesson plans ───────────────────────────────────────
create table if not exists public.plans (
  id uuid primary key default gen_random_uuid(),
  teacher_id uuid not null references public.teachers (id) on delete cascade,
  lesson_id text not null,
  day integer not null check (day >= 1),
  plan jsonb not null default '{}'::jsonb,
  selections jsonb not null default '{}'::jsonb,
  teacher_resources text not null default '',
  status plan_status not null default 'planned',
  feedback text,
  saved_at timestamptz not null default now(),
  completed_at timestamptz,
  updated_at timestamptz not null default now(),
  unique (teacher_id, lesson_id, day)
);

create index if not exists plans_teacher_idx on public.plans (teacher_id);
create index if not exists plans_lesson_idx on public.plans (teacher_id, lesson_id);

-- ─── Assessments ────────────────────────────────────────
create table if not exists public.assessments (
  id uuid primary key default gen_random_uuid(),
  teacher_id uuid not null references public.teachers (id) on delete cascade,
  type assessment_type not null,
  grade integer not null,
  subject text not null default 'english',
  group_id text,
  group_name text,
  tool_id text,
  score_percent numeric(5,2) not null default 0,
  students_assessed integer not null default 0,
  notes text,
  tallies jsonb not null default '[]'::jsonb,
  strand_scores jsonb not null default '[]'::jsonb,
  weak_items text[] not null default '{}',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists assessments_teacher_idx on public.assessments (teacher_id);
create index if not exists assessments_type_idx on public.assessments (teacher_id, type, grade);

-- ─── Classes / ability groups ───────────────────────────
create table if not exists public.classes (
  id uuid primary key default gen_random_uuid(),
  teacher_id uuid not null references public.teachers (id) on delete cascade,
  name text not null,
  grade integer not null,
  subject text not null default 'english',
  student_count integer not null default 0,
  ability_group text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists classes_teacher_idx on public.classes (teacher_id);

-- ─── Payments (server-only) ─────────────────────────────
create table if not exists public.payments (
  id uuid primary key default gen_random_uuid(),
  teacher_id uuid not null references public.teachers (id) on delete cascade,
  plan_id text not null,
  tier tier_id not null,
  billing_cycle billing_cycle not null,
  amount_paise integer not null,
  order_id text not null,
  payment_id text not null,
  created_at timestamptz not null default now(),
  unique (payment_id)
);

create index if not exists payments_teacher_idx on public.payments (teacher_id);
create index if not exists payments_order_idx on public.payments (order_id);

-- ─── Catalog: TLM ───────────────────────────────────────
create table if not exists public.tlm_resources (
  id text primary key,
  label text not null,
  emoji text not null default '',
  image_url text,
  sort_order integer not null default 0,
  updated_at timestamptz not null default now()
);

-- ─── Catalog: flashcards ────────────────────────────────
create table if not exists public.flashcard_lessons (
  lesson_id text primary key,
  title text not null default '',
  cards jsonb not null default '[]'::jsonb,
  updated_at timestamptz not null default now()
);

-- ─── updated_at helper ──────────────────────────────────
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists teachers_updated_at on public.teachers;
create trigger teachers_updated_at before update on public.teachers
  for each row execute function public.set_updated_at();

drop trigger if exists plans_updated_at on public.plans;
create trigger plans_updated_at before update on public.plans
  for each row execute function public.set_updated_at();

drop trigger if exists assessments_updated_at on public.assessments;
create trigger assessments_updated_at before update on public.assessments
  for each row execute function public.set_updated_at();

drop trigger if exists classes_updated_at on public.classes;
create trigger classes_updated_at before update on public.classes
  for each row execute function public.set_updated_at();

-- ─── Auto-create teacher row on signup ──────────────────
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.teachers (id, phone_number)
  values (
    new.id,
    coalesce(new.phone, new.raw_user_meta_data->>'phone', '')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ─── RLS ────────────────────────────────────────────────
alter table public.teachers enable row level security;
alter table public.plans enable row level security;
alter table public.assessments enable row level security;
alter table public.classes enable row level security;
alter table public.payments enable row level security;
alter table public.tlm_resources enable row level security;
alter table public.flashcard_lessons enable row level security;

drop policy if exists teachers_select_own on public.teachers;
create policy teachers_select_own on public.teachers
  for select using (auth.uid() = id);

drop policy if exists teachers_update_own on public.teachers;
create policy teachers_update_own on public.teachers
  for update using (auth.uid() = id);

drop policy if exists plans_own on public.plans;
create policy plans_own on public.plans
  for all using (auth.uid() = teacher_id) with check (auth.uid() = teacher_id);

drop policy if exists assessments_own on public.assessments;
create policy assessments_own on public.assessments
  for all using (auth.uid() = teacher_id) with check (auth.uid() = teacher_id);

drop policy if exists classes_own on public.classes;
create policy classes_own on public.classes
  for all using (auth.uid() = teacher_id) with check (auth.uid() = teacher_id);

drop policy if exists payments_select_own on public.payments;
create policy payments_select_own on public.payments
  for select using (auth.uid() = teacher_id);

drop policy if exists tlm_public_read on public.tlm_resources;
create policy tlm_public_read on public.tlm_resources
  for select using (true);

drop policy if exists flashcards_public_read on public.flashcard_lessons;
create policy flashcards_public_read on public.flashcard_lessons
  for select using (true);
