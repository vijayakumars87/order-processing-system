package com.assignment.orderprocessing.service;

import com.assignment.orderprocessing.domain.Order;
import com.assignment.orderprocessing.domain.OrderItem;
import com.assignment.orderprocessing.domain.OrderStatus;
import com.assignment.orderprocessing.domain.Product;
import com.assignment.orderprocessing.dto.CreateOrderRequest;
import com.assignment.orderprocessing.dto.OrderItemRequest;
import com.assignment.orderprocessing.exception.InvalidOrderStateException;
import com.assignment.orderprocessing.exception.ResourceNotFoundException;
import com.assignment.orderprocessing.repository.OrderRepository;
import com.assignment.orderprocessing.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private OrderService orderService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).name("Laptop").price(new BigDecimal("1000.00")).stock(5).build();
    }

    @Test
    void createOrder_decrementsStockAndComputesTotal() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(1L, 2)));
        Order order = orderService.createOrder("alice", request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCustomerUsername()).isEqualTo("alice");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("2000.00");
        assertThat(product.getStock()).isEqualTo(3);
    }

    @Test
    void createOrder_failsWhenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(99L, 1)));

        assertThatThrownBy(() -> orderService.createOrder("alice", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrder_failsWhenInsufficientStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(1L, 10)));

        assertThatThrownBy(() -> orderService.createOrder("alice", request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void updateStatus_rejectsIllegalTransition() {
        Order order = pendingOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.DELIVERED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Illegal status transition");
    }

    @Test
    void cancelOrder_succeedsForPendingAndRestoresStock() {
        Order order = pendingOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order cancelled = orderService.cancelOrder(1L);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(7); // 5 + 2 restored
    }

    @Test
    void cancelOrder_failsWhenNotPending() {
        Order order = pendingOrder();
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Only PENDING orders can be cancelled");
    }

    private Order pendingOrder() {
        Order order = Order.builder()
                .id(1L)
                .customerUsername("alice")
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        order.addItem(OrderItem.builder().product(product).quantity(2).unitPrice(product.getPrice()).build());
        return order;
    }
}
