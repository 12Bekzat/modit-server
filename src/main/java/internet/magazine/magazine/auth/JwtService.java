package internet.magazine.magazine.auth;

import internet.magazine.magazine.auth.dto.AuthResponse;
import internet.magazine.magazine.auth.dto.UserProfileResponse;
import internet.magazine.magazine.config.JwtProperties;
import internet.magazine.magazine.user.UserAccount;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public AuthResponse createAuthResponse(UserAccount user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
        String token = jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                    .issuer(jwtProperties.issuer())
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .subject(user.getEmail())
                    .claim("userId", user.getId())
                    .claim("fullName", user.getFullName())
                    .claim("roles", List.of(user.getRole().name()))
                    .build()
            )
        ).getTokenValue();

        return new AuthResponse(token, "Bearer", expiresAt, UserProfileResponse.from(user));
    }
}
