package com.synapse.social.studioasinc.feature.shared.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

private data class IdentityBadgeDefinition(
    @DrawableRes val icon: Int,
    @StringRes val name: Int,
    @StringRes val description: Int,
)

// Stable IDs and ordering match the server catalogue. Unknown IDs intentionally render nothing.
private val identityBadges = linkedMapOf(
    "verified" to IdentityBadgeDefinition(R.drawable.ic_identity_verified, R.string.identity_badge_verified_name, R.string.identity_badge_verified_description),
    "official" to IdentityBadgeDefinition(R.drawable.ic_identity_workspace_premium, R.string.identity_badge_official_name, R.string.identity_badge_official_description),
    "notable" to IdentityBadgeDefinition(R.drawable.ic_identity_stars, R.string.identity_badge_notable_name, R.string.identity_badge_notable_description),
    "trusted" to IdentityBadgeDefinition(R.drawable.ic_identity_verified_user, R.string.identity_badge_trusted_name, R.string.identity_badge_trusted_description),
    "owner" to IdentityBadgeDefinition(R.drawable.ic_identity_crown, R.string.identity_badge_owner_name, R.string.identity_badge_owner_description),
    "admin" to IdentityBadgeDefinition(R.drawable.ic_identity_admin_panel_settings, R.string.identity_badge_admin_name, R.string.identity_badge_admin_description),
    "moderator" to IdentityBadgeDefinition(R.drawable.ic_identity_shield_person, R.string.identity_badge_moderator_name, R.string.identity_badge_moderator_description),
    "staff" to IdentityBadgeDefinition(R.drawable.ic_identity_badge, R.string.identity_badge_staff_name, R.string.identity_badge_staff_description),
    "support" to IdentityBadgeDefinition(R.drawable.ic_identity_support_agent, R.string.identity_badge_support_name, R.string.identity_badge_support_description),
    "community_manager" to IdentityBadgeDefinition(R.drawable.ic_identity_groups, R.string.identity_badge_community_manager_name, R.string.identity_badge_community_manager_description),
    "developer" to IdentityBadgeDefinition(R.drawable.ic_identity_code, R.string.identity_badge_developer_name, R.string.identity_badge_developer_description),
    "core_developer" to IdentityBadgeDefinition(R.drawable.ic_identity_terminal, R.string.identity_badge_core_developer_name, R.string.identity_badge_core_developer_description),
    "contributor" to IdentityBadgeDefinition(R.drawable.ic_identity_commit, R.string.identity_badge_contributor_name, R.string.identity_badge_contributor_description),
    "open_source" to IdentityBadgeDefinition(R.drawable.ic_identity_hub, R.string.identity_badge_open_source_name, R.string.identity_badge_open_source_description),
    "technical_expert" to IdentityBadgeDefinition(R.drawable.ic_identity_engineering, R.string.identity_badge_technical_expert_name, R.string.identity_badge_technical_expert_description),
    "bug_hunter" to IdentityBadgeDefinition(R.drawable.ic_identity_bug_report, R.string.identity_badge_bug_hunter_name, R.string.identity_badge_bug_hunter_description),
    "founding_member" to IdentityBadgeDefinition(R.drawable.ic_identity_flag, R.string.identity_badge_founding_member_name, R.string.identity_badge_founding_member_description),
    "early_supporter" to IdentityBadgeDefinition(R.drawable.ic_identity_favorite, R.string.identity_badge_early_supporter_name, R.string.identity_badge_early_supporter_description),
    "community_hero" to IdentityBadgeDefinition(R.drawable.ic_identity_volunteer_activism, R.string.identity_badge_community_hero_name, R.string.identity_badge_community_hero_description),
    "ambassador" to IdentityBadgeDefinition(R.drawable.ic_identity_public, R.string.identity_badge_ambassador_name, R.string.identity_badge_ambassador_description),
    "beta_tester" to IdentityBadgeDefinition(R.drawable.ic_identity_science, R.string.identity_badge_beta_tester_name, R.string.identity_badge_beta_tester_description),
    "creator" to IdentityBadgeDefinition(R.drawable.ic_identity_auto_awesome, R.string.identity_badge_creator_name, R.string.identity_badge_creator_description),
    "video_creator" to IdentityBadgeDefinition(R.drawable.ic_identity_videocam, R.string.identity_badge_video_creator_name, R.string.identity_badge_video_creator_description),
    "influencer" to IdentityBadgeDefinition(R.drawable.ic_identity_campaign, R.string.identity_badge_influencer_name, R.string.identity_badge_influencer_description),
    "featured_creator" to IdentityBadgeDefinition(R.drawable.ic_identity_featured_play_list, R.string.identity_badge_featured_creator_name, R.string.identity_badge_featured_creator_description),
    "top_creator" to IdentityBadgeDefinition(R.drawable.ic_identity_emoji_events, R.string.identity_badge_top_creator_name, R.string.identity_badge_top_creator_description),
    "rising_creator" to IdentityBadgeDefinition(R.drawable.ic_identity_trending_up, R.string.identity_badge_rising_creator_name, R.string.identity_badge_rising_creator_description),
    "partner" to IdentityBadgeDefinition(R.drawable.ic_identity_handshake, R.string.identity_badge_partner_name, R.string.identity_badge_partner_description),
    "premium" to IdentityBadgeDefinition(R.drawable.ic_identity_workspace_premium, R.string.identity_badge_premium_name, R.string.identity_badge_premium_description),
    "vip" to IdentityBadgeDefinition(R.drawable.ic_identity_diamond, R.string.identity_badge_vip_name, R.string.identity_badge_vip_description),
    "special_guest" to IdentityBadgeDefinition(R.drawable.ic_identity_celebration, R.string.identity_badge_special_guest_name, R.string.identity_badge_special_guest_description),
    "legend" to IdentityBadgeDefinition(R.drawable.ic_identity_trophy, R.string.identity_badge_legend_name, R.string.identity_badge_legend_description),
    "pioneer" to IdentityBadgeDefinition(R.drawable.ic_identity_rocket_launch, R.string.identity_badge_pioneer_name, R.string.identity_badge_pioneer_description),
)

/** Icon-only inline badge. The activated menu provides an accessible name and explanation. */
@Composable
fun IdentityBadge(badgeType: String?, modifier: Modifier = Modifier) {
    val badge = identityBadges[badgeType] ?: return
    var expanded by remember(badgeType) { mutableStateOf(false) }
    val label = stringResource(badge.name)
    val explanation = stringResource(badge.description)

    Box(modifier = modifier) {
        Icon(
            painter = painterResource(badge.icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Sizes.IconLarge).clickable(role = Role.Button) { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = "$label — $explanation") },
                onClick = { expanded = false }
            )
        }
    }
}
