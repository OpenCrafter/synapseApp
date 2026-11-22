-- Run after the staging migration in an isolated database.
begin;
do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conrelid = 'private.user_badges'::regclass
      and contype = 'p' and pg_get_constraintdef(oid) = 'PRIMARY KEY (user_id)'
  ) then
    raise exception 'Badge assignments must be unique per user';
  end if;
  if not exists (
    select 1 from pg_class where oid = 'private.user_badges'::regclass and relrowsecurity
  ) then
    raise exception 'Badge assignments must have RLS enabled';
  end if;
  if has_table_privilege('authenticated', 'private.user_badges', 'INSERT,UPDATE,DELETE')
     or has_table_privilege('anon', 'private.user_badges', 'INSERT,UPDATE,DELETE') then
    raise exception 'Client roles must not be able to write badges';
  end if;
end;
$$;
rollback;
