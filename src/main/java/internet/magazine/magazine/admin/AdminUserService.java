package internet.magazine.magazine.admin;

import internet.magazine.magazine.admin.dto.AdminUserCreateRequest;
import internet.magazine.magazine.admin.dto.AdminUserUpdateRequest;
import internet.magazine.magazine.auth.EmailAlreadyExistsException;
import internet.magazine.magazine.auth.dto.UserProfileResponse;
import internet.magazine.magazine.common.ResourceNotFoundException;
import internet.magazine.magazine.user.UserAccount;
import internet.magazine.magazine.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> findAll() {
        return userRepository.findAll()
            .stream()
            .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
            .map(UserProfileResponse::from)
            .toList();
    }

    @Transactional
    public UserProfileResponse create(AdminUserCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone().trim());
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(Instant.now());

        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse update(Long id, AdminUserUpdateRequest request, String currentAdminEmail) {
        UserAccount user = getUser(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new EmailAlreadyExistsException(email);
        }

        if (currentAdminEmail.equalsIgnoreCase(user.getEmail()) && request.role() != user.getRole()) {
            throw new IllegalStateException("You cannot change your own role.");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone().trim());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, String currentAdminEmail) {
        UserAccount user = getUser(id);
        if (currentAdminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalStateException("You cannot delete your own administrator account.");
        }
        userRepository.delete(user);
    }

    private UserAccount getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id '%s' was not found.".formatted(id)));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
