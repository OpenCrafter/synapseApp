# Identity badges: Android profile rollout

The Android profile header reads one nullable badge ID from the existing
`get_profile_badge_types` visibility-checked RPC. A missing row or unknown ID
renders no badge. Refreshing/reopening a profile re-reads the authoritative
assignment; a failed lookup fails the profile load rather than displaying cached
or client-supplied privilege. No client assignment API is exposed.

The production `private.user_badges` table has a primary key on user_id, an
allowlist constraint, and a service-role-only assignment RPC. Badge replacement
and removal use that server-controlled workflow. An empty response may also mean
the viewer cannot see a private profile; it must not be used to infer verification.
The legacy `verify` field is not used as a badge fallback because it is client-writable under the inspected grants/policies and doing so could
re-display a revoked or replaced badge. Migrating any legacy verified accounts
requires an authorized separate backend rollout; this change does not modify
production data. As of the inspected environment, aggregate counts showed zero
legacy verified accounts and zero badge assignments; that is not a guarantee for
other environments.

## Catalogue

Order is the issue's management order. IDs are stable; the source records who
may authorize an assignment, not a client-enforced entitlement check. Premium
and Partner must be backed by authoritative entitlement/partnership workflows
before assignment. The current admin/service role workflow must validate this.

| ID | Label | Category | Google Rounded symbol | Assignment source |
|---|---|---|---|---|
| verified | Verified | Verification | verified | verification |
| official | Official | Verification | workspace_premium | admin |
| notable | Notable | Verification | stars | admin |
| trusted | Trusted | Verification | verified_user | admin |
| owner | Owner | Staff | crown | admin |
| admin | Admin | Staff | admin_panel_settings | admin |
| moderator | Moderator | Staff | shield_person | admin |
| staff | Staff | Staff | badge | admin |
| support | Support | Staff | support_agent | admin |
| community_manager | Community Manager | Staff | groups | admin |
| developer | Developer | Technical | code | admin |
| core_developer | Core Developer | Technical | terminal | admin |
| contributor | Contributor | Technical | commit | admin |
| open_source | Open Source | Technical | hub | admin |
| technical_expert | Technical Expert | Technical | engineering | admin |
| bug_hunter | Bug Hunter | Technical | bug_report | admin |
| founding_member | Founding Member | Community | flag | admin |
| early_supporter | Early Supporter | Community | favorite | admin |
| community_hero | Community Hero | Community | volunteer_activism | admin |
| ambassador | Ambassador | Community | public | admin |
| beta_tester | Beta Tester | Community | science | admin |
| creator | Creator | Creator | auto_awesome | admin |
| video_creator | Video Creator | Creator | videocam | admin |
| influencer | Influencer | Creator | campaign | admin |
| featured_creator | Featured Creator | Creator | featured_play_list | admin |
| top_creator | Top Creator | Creator | emoji_events | admin |
| rising_creator | Rising Creator | Creator | trending_up | admin |
| partner | Partner | Affiliation | handshake | partnership |
| premium | Premium | Affiliation | workspace_premium | entitlement |
| vip | VIP | Affiliation | diamond | admin |
| special_guest | Special Guest | Affiliation | celebration | admin |
| legend | Legend | Affiliation | trophy | admin |
| pioneer | Pioneer | Affiliation | rocket_launch | admin |

The 32 distinct vector paths were fetched from Google's Material Symbols
Rounded icon assets (24px default): `https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/<symbol>/default/24px.svg`.
All listed symbols returned successfully on 2026-11-24; Official and Premium
share `workspace_premium`. No substitutions were needed. The vector paths are
bundled with the app; no runtime icon request is required. Google Material
Symbols are distributed under Apache License 2.0:
https://github.com/google/material-design-icons/blob/master/LICENSE

To add a future identity badge, agree the server authorization/eligibility and
privacy rules, extend the server allowlist and admin workflow first, then append
the stable ID, icon and localized name/description to the Android catalogue.
Verify a Google Rounded vector, bundle it, and check compact and large-font
layouts. Do not add achievements or a client self-assignment control.

## Outstanding

This is a profile-header slice only: feed, search, replies, messaging, member
lists, iOS and web do not yet consume these assignments. Existing legacy
verification surfaces still require migration, and clients do not receive
instant updates while a screen stays open. Android compilation, device/preview
layout, screen reader and contrast validation must be completed before this
issue is marked complete.
