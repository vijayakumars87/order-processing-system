package com.assignment.orderprocessing.domain;

import java.util.Set;

/**
 * Order lifecycle states and the legal transitions between them.
 * PENDING -> PROCESSING -> SHIPPED -> DELIVERED
 * PENDING -> CANCELLED (only PENDING orders may be cancelled)
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> ALLOWED = java.util.Map.of(
            PENDING, Set.of(PROCESSING, CANCELLED),
            PROCESSING, Set.of(SHIPPED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
