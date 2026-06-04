package com.assignment.orderprocessing.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void pendingCanGoToProcessingAndCancelled() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void pendingCannotSkipToShippedOrDelivered() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    void happyPathTransitions() {
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    void terminalStatesHaveNoTransitions() {
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PROCESSING)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PROCESSING)).isFalse();
    }

    @Test
    void processingCannotBeCancelled() {
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }
}
