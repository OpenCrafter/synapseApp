# Issue #821 — badge rollout

Supabase owns `badge_type`. The private assignments table has one primary-keyed row per profile. No badge SQL is maintained in this repository.

- The service-role-only `set_user_badge_type` operation inserts or replaces one badge atomically; null revokes it. The trusted caller must validate eligibility against authoritative role, verification, entitlement, or partnership records before assignment. Never authorize app privileges from badge labels.
- `get_profile_badge_types` accepts up to 200 profile UIDs and respects the existing profile visibility policy. Do not retain a cached badge when a fresh lookup no longer returns the assignment.
- Web profile headers consume this endpoint and use locally bundled 24 px Google Material Symbols Rounded SVGs. Official and Premium share `workspace_premium`; all other names use their corresponding requested symbols. The upstream license is bundled with the SVGs.

## Before cutover

1. Reconcile verification against independently approved records. Do not backfill from `verify`, `account_premium`, or `account_type`: authenticated users currently have permission to write those fields on their own profiles.
2. Connect trusted assignment callers, including authoritative entitlement and partnership checks. Replace legacy verification rendering only after reconciliation and wire all identity surfaces to authoritative badge reads.
3. Verify replacement and revocation, denied client access, visibility, cache refresh, and responsive accessible layouts. Run builds for Android, iOS, and web before marking the PR ready.
