package internet.magazine.magazine.auth.dto;

import internet.magazine.magazine.user.UserAccount;
import java.time.Instant;

public record UserProfileResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    String role,
    String initials,
    Instant createdAt
) {

    public static UserProfileResponse from(UserAccount user) {
        return new UserProfileResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole().name(),
            buildInitials(user.getFullName()),
            user.getCreatedAt()
        );
    }

    private static String buildInitials(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.toString();
    }
}
