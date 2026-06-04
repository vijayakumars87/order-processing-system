package com.assignment.orderprocessing.scheduler;

import com.assignment.orderprocessing.domain.Order;
import com.assignment.orderprocessing.domain.OrderStatus;
import com.assignment.orderprocessing.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Background job that promotes PENDING orders to PROCESSING.
 * Runs on the cron defined by {@code order.scheduler.pending-to-processing-cron}
 * (default: every 5 minutes).
 */
@Component
public class OrderStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusScheduler.class);

    private final OrderRepository orderRepository;

    public OrderStatusScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(cron = "${order.scheduler.pending-to-processing-cron}")
    @Transactional
    public void promotePendingOrders() {
        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (Order order : pending) {
            order.setStatus(OrderStatus.PROCESSING);
            order.setUpdatedAt(now);
        }
        orderRepository.saveAll(pending);
        log.info("Promoted {} PENDING order(s) to PROCESSING", pending.size());
    }
}
