-- PedaStudio — make teacher ids provider-agnostic text instead of Supabase-auth uuids.
--
-- Why: the API identifies teachers by the uid inside the ID token it receives.
-- Today that is a Firebase uid (e.g. "kR2mQ9xTv1abCdEfGhIjKlMnOpQ2"), which is not a
-- uuid, so every insert into teachers failed with
--   invalid input syntax for type uuid
-- Text ids accept Firebase uids now and Supabase auth uuids later, unchanged.
--
-- Safe to re-run. Run in Supabase SQL Editor.

-- ─── Drop policies first ────────────────────────────────
-- Postgres refuses to alter a column that a policy definition references, so the
-- policies come off here and are recreated at the bottom.
drop policy if exists teachers_select_own on public.teachers;
drop policy if exists teachers_update_own on public.teachers;
drop policy if exists plans_own on public.plans;
drop policy if exists assessments_own on public.assessments;
drop policy if exists classes_own on public.classes;
drop policy if exists payments_select_own on public.payments;

-- ─── Stop depending on auth.users ────────────────────────
drop trigger if exists on_auth_user_created on auth.users;
drop function if exists public.handle_new_user();

alter table public.teachers drop constraint if exists teachers_id_fkey;

-- ─── uuid → text (drop child FKs, convert, restore) ─────
alter table public.plans drop constraint if exists plans_teacher_id_fkey;
alter table public.assessments drop constraint if exists assessments_teacher_id_fkey;
alter table public.classes drop constraint if exists classes_teacher_id_fkey;
alter table public.payments drop constraint if exists payments_teacher_id_fkey;

alter table public.teachers alter column id type text using id::text;
alter table public.plans alter column teacher_id type text using teacher_id::text;
alter table public.assessments alter column teacher_id type text using teacher_id::text;
alter table public.classes alter column teacher_id type text using teacher_id::text;
alter table public.payments alter column teacher_id type text using teacher_id::text;

alter table public.plans
  add constraint plans_teacher_id_fkey foreign key (teacher_id)
  references public.teachers (id) on delete cascade;
alter table public.assessments
  add constraint assessments_teacher_id_fkey foreign key (teacher_id)
  references public.teachers (id) on delete cascade;
alter table public.classes
  add constraint classes_teacher_id_fkey foreign key (teacher_id)
  references public.teachers (id) on delete cascade;
alter table public.payments
  add constraint payments_teacher_id_fkey foreign key (teacher_id)
  references public.teachers (id) on delete cascade;

-- ─── One assessment record per teacher + type + grade + group ────
-- Firestore keyed these as "baseline" / "endline" / "unit_<groupId>" per teacher, so a
-- teacher handling two grades overwrote their own baseline. Including grade fixes that.
-- group_id is '' rather than null so ON CONFLICT can use a plain unique constraint.
update public.assessments set group_id = '' where group_id is null;
alter table public.assessments alter column group_id set default '';
alter table public.assessments alter column group_id set not null;

alter table public.assessments drop constraint if exists assessments_unique_key;
alter table public.assessments
  add constraint assessments_unique_key unique (teacher_id, type, grade, group_id);

-- ─── Recreate policies, comparing auth.uid() as text ────
create policy teachers_select_own on public.teachers
  for select using (auth.uid()::text = id);

create policy teachers_update_own on public.teachers
  for update using (auth.uid()::text = id);

create policy plans_own on public.plans
  for all using (auth.uid()::text = teacher_id) with check (auth.uid()::text = teacher_id);

create policy assessments_own on public.assessments
  for all using (auth.uid()::text = teacher_id) with check (auth.uid()::text = teacher_id);

create policy classes_own on public.classes
  for all using (auth.uid()::text = teacher_id) with check (auth.uid()::text = teacher_id);

create policy payments_select_own on public.payments
  for select using (auth.uid()::text = teacher_id);
