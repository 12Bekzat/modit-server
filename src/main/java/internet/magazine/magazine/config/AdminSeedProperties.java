package internet.magazine.magazine.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminSeedProperties(
    @NotBlank
    String email,
    @NotBlank @Size(min = 12)
    String password,
    @NotBlank
    String fullName,
    @NotBlank
    String phone
) {
}
