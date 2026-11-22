import Image from "next/image";

// Google Material Symbols Rounded, 24px, bundled in public/badges.
const catalogue = new Map<string, readonly [string, string]>([
    ["verified", ["Verified", "verified"]],
    ["official", ["Official", "workspace_premium"]],
    ["notable", ["Notable", "stars"]],
    ["trusted", ["Trusted", "verified_user"]],
    ["owner", ["Owner", "crown"]],
    ["admin", ["Admin", "admin_panel_settings"]],
    ["moderator", ["Moderator", "shield_person"]],
    ["staff", ["Staff", "badge"]],
    ["support", ["Support", "support_agent"]],
    ["community_manager", ["Community Manager", "groups"]],
    ["developer", ["Developer", "code"]],
    ["core_developer", ["Core Developer", "terminal"]],
    ["contributor", ["Contributor", "commit"]],
    ["open_source", ["Open Source", "hub"]],
    ["technical_expert", ["Technical Expert", "engineering"]],
    ["bug_hunter", ["Bug Hunter", "bug_report"]],
    ["founding_member", ["Founding Member", "flag"]],
    ["early_supporter", ["Early Supporter", "favorite"]],
    ["community_hero", ["Community Hero", "volunteer_activism"]],
    ["ambassador", ["Ambassador", "public"]],
    ["beta_tester", ["Beta Tester", "science"]],
    ["creator", ["Creator", "auto_awesome"]],
    ["video_creator", ["Video Creator", "videocam"]],
    ["influencer", ["Influencer", "campaign"]],
    ["featured_creator", ["Featured Creator", "featured_play_list"]],
    ["top_creator", ["Top Creator", "emoji_events"]],
    ["rising_creator", ["Rising Creator", "trending_up"]],
    ["partner", ["Partner", "handshake"]],
    ["premium", ["Premium", "workspace_premium"]],
    ["vip", ["VIP", "diamond"]],
    ["special_guest", ["Special Guest", "celebration"]],
    ["legend", ["Legend", "trophy"]],
    ["pioneer", ["Pioneer", "rocket_launch"]],
]);

export const UserBadge = ({ type }: { type: string }) => {
    const entry = catalogue.get(type);
    if (!entry) return null;
    const [name, icon] = entry;
    const label = `Account badge: ${name}`;
    return (
        <span className="inline-flex size-6 shrink-0 items-center justify-center rounded focus-visible:outline" tabIndex={0} title={label}>
            <Image unoptimized src={"/badges/" + icon + ".svg"} width={20} height={20} className="dark:invert" alt={label} />
        </span>
    );
};
