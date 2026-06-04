package com.assignment.orderprocessing;

import com.assignment.orderprocessing.domain.Product;
import com.assignment.orderprocessing.domain.Role;
import com.assignment.orderprocessing.domain.User;
import com.assignment.orderprocessing.dto.AuthResponse;
import com.assignment.orderprocessing.repository.OrderRepository;
import com.assignment.orderprocessing.repository.ProductRepository;
import com.assignment.orderprocessing.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long productId;

    @BeforeEach
    void seed() {
        // Delete in FK-safe order: orders (and their items) before products/users.
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.save(User.builder().username("admin").password(passwordEncoder.encode("admin123"))
                .email("admin@example.com").fullName("System Admin").mobileNumber("+10000000000")
                .role(Role.ADMIN).enabled(true).build());
        userRepository.save(User.builder().username("alice").password(passwordEncoder.encode("alice123"))
                .email("alice@example.com").fullName("Alice Customer").mobileNumber("+10000000002")
                .role(Role.CUSTOMER).enabled(true).build());
        productId = productRepository.save(
                Product.builder().name("Laptop").price(new BigDecimal("1000.00")).stock(5).build()).getId();
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullOrderLifecycle() throws Exception {
        String customerToken = login("alice", "alice123");
        String adminToken = login("admin", "admin123");

        // Create order as customer
        String createBody = """
                {"items":[{"productId":%d,"quantity":2}]}
                """.formatted(productId);
        String createResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(2000.00))
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(createResponse).get("id").asLong();

        // Customer can retrieve their own order
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Customer cannot update status (ADMIN only) -> 403
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isForbidden());

        // Admin moves PENDING -> PROCESSING
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        // Cancel now fails because it is no longer PENDING -> 409
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelPendingOrderSucceeds() throws Exception {
        String customerToken = login("alice", "alice123");
        String createBody = """
                {"items":[{"productId":%d,"quantity":1}]}
                """.formatted(productId);
        String createResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void createOrderWithInvalidQuantityFailsValidation() throws Exception {
        String customerToken = login("alice", "alice123");
        String createBody = """
                {"items":[{"productId":%d,"quantity":0}]}
                """.formatted(productId);
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest());
    }

    private String login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, AuthResponse.class).token();
    }
}
