package com.assignment.orderprocessing.scheduler;

import com.assignment.orderprocessing.domain.Order;
import com.assignment.orderprocessing.domain.OrderStatus;
import com.assignment.orderprocessing.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderStatusSchedulerTest {

    @Autowired
    private OrderStatusScheduler scheduler;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void promotePendingOrders_movesPendingToProcessing() {
        Order pending = saveOrder(OrderStatus.PENDING);
        Order shipped = saveOrder(OrderStatus.SHIPPED);

        scheduler.promotePendingOrders();

        assertThat(orderRepository.findById(pending.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PROCESSING);
        assertThat(orderRepository.findById(shipped.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.SHIPPED);
    }

    private Order saveOrder(OrderStatus status) {
        Instant now = Instant.now();
        return orderRepository.save(Order.builder()
                .customerUsername("alice")
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
