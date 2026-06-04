package com.assignment.orderprocessing.config;

import com.assignment.orderprocessing.domain.Order;
import com.assignment.orderprocessing.domain.OrderItem;
import com.assignment.orderprocessing.domain.OrderStatus;
import com.assignment.orderprocessing.domain.Product;
import com.assignment.orderprocessing.domain.Role;
import com.assignment.orderprocessing.domain.User;
import com.assignment.orderprocessing.repository.OrderRepository;
import com.assignment.orderprocessing.repository.ProductRepository;
import com.assignment.orderprocessing.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds realistic demo data (users, product catalogue and orders covering every status)
 * so all functionality can be exercised immediately after startup.
 * Disabled under the "test" profile to keep tests deterministic.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      ProductRepository productRepository,
                      OrderRepository orderRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded
        }

        seedUsers();
        Map<String, Product> products = seedProducts();
        seedOrders(products);

        log.info("Seeded {} users, {} products, {} orders",
                userRepository.count(), productRepository.count(), orderRepository.count());
    }

    private void seedUsers() {
        userRepository.save(user("admin", "admin123", "admin@example.com", "System Admin", "+919000000000", Role.ADMIN, true));
        userRepository.save(user("customer", "customer123", "customer@example.com", "Default Customer", "+919000000001", Role.CUSTOMER, true));
        userRepository.save(user("rahul", "rahul123", "rahul@example.com", "Rahul Sharma", "+919812345678", Role.CUSTOMER, true));
        userRepository.save(user("priya", "priya123", "priya@example.com", "Priya Nair", "+919876543210", Role.CUSTOMER, true));
        // A disabled account to test that login is blocked.
        userRepository.save(user("blocked", "blocked123", "blocked@example.com", "Blocked User", "+919999999999", Role.CUSTOMER, false));
    }

    private Map<String, Product> seedProducts() {
        Map<String, Product> products = new LinkedHashMap<>();
        save(products, "Mobile - iPhone 15", "79999.00", 40);
        save(products, "Mobile - Samsung Galaxy S24", "74999.00", 35);
        save(products, "Air Conditioner - 1.5 Ton Split", "38999.00", 20);
        save(products, "Laptop - Dell XPS 15", "145000.00", 15);
        save(products, "Laptop - MacBook Air M3", "114900.00", 18);
        save(products, "Smart TV - 55\" 4K OLED", "89999.00", 12);
        save(products, "Refrigerator - 260L Double Door", "27999.00", 25);
        save(products, "Washing Machine - 7kg Front Load", "32999.00", 22);
        save(products, "Headphones - Sony WH-1000XM5", "29990.00", 60);
        save(products, "Tablet - iPad Air", "59900.00", 30);
        save(products, "Microwave Oven - 30L Convection", "14999.00", 28);
        save(products, "Wireless Mouse - Logitech MX", "8995.00", 120);
        return products;
    }

    private void seedOrders(Map<String, Product> p) {
        Instant now = Instant.now();

        // PENDING - eligible for cancellation and for the scheduler to pick up.
        orderRepository.save(order("customer", OrderStatus.PENDING, now.minus(2, ChronoUnit.MINUTES),
                item(p.get("Mobile - iPhone 15"), 1),
                item(p.get("Headphones - Sony WH-1000XM5"), 1)));

        orderRepository.save(order("rahul", OrderStatus.PENDING, now.minus(1, ChronoUnit.MINUTES),
                item(p.get("Wireless Mouse - Logitech MX"), 2)));

        // PROCESSING
        orderRepository.save(order("priya", OrderStatus.PROCESSING, now.minus(30, ChronoUnit.MINUTES),
                item(p.get("Laptop - MacBook Air M3"), 1)));

        // SHIPPED
        orderRepository.save(order("rahul", OrderStatus.SHIPPED, now.minus(2, ChronoUnit.DAYS),
                item(p.get("Air Conditioner - 1.5 Ton Split"), 1),
                item(p.get("Smart TV - 55\" 4K OLED"), 1)));

        // DELIVERED
        orderRepository.save(order("customer", OrderStatus.DELIVERED, now.minus(7, ChronoUnit.DAYS),
                item(p.get("Refrigerator - 260L Double Door"), 1),
                item(p.get("Microwave Oven - 30L Convection"), 1)));

        // CANCELLED - stock is reserved then restored, mirroring a real cancellation.
        OrderItem cancelledItem = item(p.get("Tablet - iPad Air"), 1);
        restoreStock(cancelledItem);
        orderRepository.save(order("priya", OrderStatus.CANCELLED, now.minus(3, ChronoUnit.DAYS), cancelledItem));
    }

    // ----- helpers -----

    private User user(String username, String rawPassword, String email,
                      String fullName, String mobile, Role role, boolean enabled) {
        return User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .email(email)
                .fullName(fullName)
                .mobileNumber(mobile)
                .role(role)
                .enabled(enabled)
                .build();
    }

    private void save(Map<String, Product> target, String name, String price, int stock) {
        Product product = productRepository.save(Product.builder()
                .name(name)
                .price(new BigDecimal(price))
                .stock(stock)
                .build());
        target.put(name, product);
    }

    private OrderItem item(Product product, int quantity) {
        // Reserve stock for non-cancelled demo orders to keep inventory believable.
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
        return OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .build();
    }

    private void restoreStock(OrderItem item) {
        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());
        productRepository.save(product);
    }

    private Order order(String customerUsername, OrderStatus status, Instant createdAt, OrderItem... items) {
        Order order = Order.builder()
                .customerUsername(customerUsername)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        for (OrderItem it : items) {
            order.addItem(it);
        }
        return order;
    }
}
