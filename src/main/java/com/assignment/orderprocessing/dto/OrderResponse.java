package com.assignment.orderprocessing.dto;

import com.assignment.orderprocessing.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerUsername,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {
}
