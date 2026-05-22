package internet.magazine.magazine.config;

import internet.magazine.magazine.product.Product;
import internet.magazine.magazine.product.ProductCodeGenerator;
import internet.magazine.magazine.product.ProductRepository;
import internet.magazine.magazine.product.ProductSource;
import internet.magazine.magazine.user.UserAccount;
import internet.magazine.magazine.user.UserRepository;
import internet.magazine.magazine.user.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    ApplicationRunner seedData(
        UserRepository userRepository,
        ProductRepository productRepository,
        ProductCodeGenerator productCodeGenerator,
        PasswordEncoder passwordEncoder,
        AdminSeedProperties adminSeedProperties
    ) {
        return args -> {
            seedAdmin(userRepository, passwordEncoder, adminSeedProperties);
            seedProducts(productRepository, productCodeGenerator);
            ensureProductCodes(productRepository, productCodeGenerator);
        };
    }

    private void seedAdmin(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AdminSeedProperties adminSeedProperties
    ) {
        if (userRepository.existsByEmailIgnoreCase(adminSeedProperties.email())) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setFullName(adminSeedProperties.fullName());
        admin.setEmail(adminSeedProperties.email().trim().toLowerCase());
        admin.setPhone(adminSeedProperties.phone());
        admin.setPasswordHash(passwordEncoder.encode(adminSeedProperties.password()));
        admin.setRole(UserRole.ADMIN);
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);
    }

    private void seedProducts(ProductRepository productRepository, ProductCodeGenerator productCodeGenerator) {
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> products = List.of(
            product(productCodeGenerator, "Nova X9 Pro 256 ГБ", "Смартфоны", "Nova", 249990, 289990, 4.8, true, "завтра", "Новинка",
                "Флагманский смартфон с ярким экраном и высокой автономностью."),
            product(productCodeGenerator, "Nova Lite 128 ГБ", "Смартфоны", "Nova", 139990, 159990, 4.6, true, "сегодня", "Хит",
                "Практичный смартфон на каждый день с быстрой зарядкой."),
            product(productCodeGenerator, "Vision 55 OLED", "Телевизоры", "Vision", 399990, 459990, 4.7, true, "2-3 дня", "Лучшая цена",
                "OLED-телевизор для кино, спорта и консолей нового поколения."),
            product(productCodeGenerator, "PulseBook 16 Studio", "Ноутбуки", "Pulse", 429990, 479990, 4.9, true, "завтра", "Премиум",
                "Производительный ноутбук для дизайна, монтажа и разработки."),
            product(productCodeGenerator, "CleanBot S12", "Бытовая техника", "CleanBot", 179990, 199990, 4.6, true, "завтра", "Суперсила",
                "Робот-пылесос с влажной уборкой и умной навигацией."),
            product(productCodeGenerator, "SmartChef Pro", "Кухня", "SmartChef", 139990, 159990, 4.6, true, "сегодня", "Новинка",
                "Кухонная станция с автопрограммами и управлением через приложение."),
            product(productCodeGenerator, "GameBox Neo", "Игровые решения", "GameBox", 229990, 249990, 4.9, true, "завтра", "Гейминг",
                "Игровая консоль нового поколения с SSD и 4K-графикой."),
            product(productCodeGenerator, "AirSound Studio Max", "Аудио", "AirSound", 129990, 149990, 4.9, true, "сегодня", "Хит",
                "Полноразмерные наушники с шумоподавлением и глубоким звуком."),
            product(productCodeGenerator, "SmartHome Hub 2", "Умный дом", "ТехноНорд", 59990, 69990, 4.4, true, "сегодня", "База",
                "Центр управления освещением, камерами и сценариями дома."),
            product(productCodeGenerator, "Nova Tab 11", "Планшеты", "Nova", 189990, 209990, 4.5, true, "2-3 дня", "Универсал",
                "Планшет для учебы, видео и рабочих заметок."),
            product(productCodeGenerator, "PulseBook 13 Slim", "Ноутбуки", "Pulse", 259990, 289990, 4.5, true, "завтра", "Тонкий",
                "Легкий ноутбук для дороги и офисной работы."),
            product(productCodeGenerator, "Orion Wave 2", "Аудио", "Orion", 99990, 119990, 4.4, true, "2-3 дня", "Баланс",
                "Беспроводная акустика для дома с мощным стереозвуком.")
        );

        productRepository.saveAll(products);
    }

    private void ensureProductCodes(ProductRepository productRepository, ProductCodeGenerator productCodeGenerator) {
        List<Product> productsWithoutCode = productRepository.findAll()
            .stream()
            .filter(product -> product.getProductCode() == null || product.getProductCode().isBlank())
            .peek(product -> {
                if (product.getExternalCode() != null && !product.getExternalCode().isBlank()) {
                    product.setProductCode(productCodeGenerator.generateImportedCode(product.getSource(), product.getExternalCode()));
                } else {
                    product.setProductCode(productCodeGenerator.generateManualCode());
                }
            })
            .toList();

        if (!productsWithoutCode.isEmpty()) {
            productRepository.saveAll(productsWithoutCode);
        }
    }

    private Product product(
        ProductCodeGenerator productCodeGenerator,
        String name,
        String category,
        String brand,
        int price,
        int oldPrice,
        double rating,
        boolean inStock,
        String delivery,
        String tag,
        String description
    ) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setBrand(brand);
        product.setPrice(BigDecimal.valueOf(price));
        product.setOldPrice(BigDecimal.valueOf(oldPrice));
        product.setRating(BigDecimal.valueOf(rating));
        product.setAvailableQuantity(inStock ? 10 : 0);
        product.setInStock(inStock);
        product.setDelivery(delivery);
        product.setTag(tag);
        product.setDescription(description);
        product.setSource(ProductSource.MANUAL);
        product.setProductCode(productCodeGenerator.generateManualCode());
        return product;
    }
}
