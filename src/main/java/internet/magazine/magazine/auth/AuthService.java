package internet.magazine.magazine.auth;

import internet.magazine.magazine.auth.dto.AuthResponse;
import internet.magazine.magazine.auth.dto.LoginRequest;
import internet.magazine.magazine.auth.dto.RegisterRequest;
import internet.magazine.magazine.auth.dto.UserProfileResponse;
import internet.magazine.magazine.user.UserAccount;
import internet.magazine.magazine.user.UserRole;
import internet.magazine.magazine.user.UserRepository;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setCreatedAt(Instant.now());

        UserAccount savedUser = userRepository.save(user);
        return jwtService.createAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
        );

        UserAccount user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UserNotFoundException(email));

        return jwtService.createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(String email) {
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UserNotFoundException(email));

        return UserProfileResponse.from(user);
    }
}
