import SwiftUI
import shared

struct UserResultRow: View {
    let user: SearchAccount
    @State private var showComingSoon = false

    var body: some View {
        HStack(spacing: AppTheme.Spacing.space12) {
            // Avatar Placeholder
            Circle()
                .fill(Color.gray.opacity(0.3))
                .frame(width: 48, height: 48)
                .overlay(
                    Text(String((user.displayName ?? user.handle)?.first ?? "?").uppercased())
                        .foregroundColor(.gray)
                        .font(.headline)
                )

            VStack(alignment: .leading, spacing: AppTheme.Spacing.space4) {
                HStack(alignment: .center, spacing: AppTheme.Spacing.space4) {
                    Text(user.displayName ?? user.handle ?? "Unknown User")
                        .font(.headline)
                        .lineLimit(1)

                    if user.isVerified {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundColor(.blue)
                            .font(.system(size: 14))
                    }
                }

                Text("@\(user.handle ?? "")")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)

                if let bio = user.bio, !bio.isEmpty {
                    Text(bio)
                        .font(.caption)
                        .foregroundColor(.primary)
                        .lineLimit(2)
                        .padding(.top, AppTheme.Spacing.space2)
                }
            }

            Spacer()

            // Follow button
            Button(action: {
                guard !user.isFollowing else { return }
                showComingSoon = true
            }) {
                Text(user.isFollowing ? "Following" : "Follow")
                    .font(.system(size: 14, weight: .semibold))
                    .padding(.horizontal, AppTheme.Spacing.space16)
                    .padding(.vertical, AppTheme.Spacing.space6)
                    .background(user.isFollowing ? Color.clear : Color.blue)
                    .foregroundColor(user.isFollowing ? .primary : .white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(user.isFollowing ? Color.gray.opacity(0.5) : Color.clear, lineWidth: 1)
                    )
                    .cornerRadius(16)
            }
        }
        .padding(.vertical, AppTheme.Spacing.space8)
        .padding(.horizontal)
        .alert("Coming Soon", isPresented: $showComingSoon) {
            Button("OK", role: .cancel) { }
        } message: {
            Text("Follow functionality is coming soon.")
        }
    }
}
