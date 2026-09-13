-- Recreate the signup trigger now that teachers.id is text.
-- When a teacher signs in with Supabase Phone Auth, a teachers row is created
-- automatically. Safe to re-run.

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.teachers (id, phone_number)
  values (
    new.id::text,
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
