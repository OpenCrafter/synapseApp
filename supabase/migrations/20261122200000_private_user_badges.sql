-- Issue #821 foundation. No legacy flags are trusted or backfilled.
begin;
create schema if not exists private;
create table private.user_badges (
  user_id uuid primary key references public.users(id) on delete cascade,
  badge_id text not null check (badge_id in ('verified', 'official', 'notable', 'trusted', 'owner', 'admin', 'moderator', 'staff', 'support', 'community_manager', 'developer', 'core_developer', 'contributor', 'open_source', 'technical_expert', 'bug_hunter', 'founding_member', 'early_supporter', 'community_hero', 'ambassador', 'beta_tester', 'creator', 'video_creator', 'influencer', 'featured_creator', 'top_creator', 'rising_creator', 'partner', 'premium', 'vip', 'special_guest', 'legend', 'pioneer'))
);
alter table private.user_badges enable row level security;
revoke all on private.user_badges from public, anon, authenticated, service_role;
-- Trusted database operators only until an authorized assignment workflow exists.
comment on table private.user_badges is 'One active badge per account. No client access; never seed from writable profile flags.';
commit;
