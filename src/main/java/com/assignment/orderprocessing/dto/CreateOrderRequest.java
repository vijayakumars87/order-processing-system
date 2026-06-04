package com.assignment.orderprocessing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "an order must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {
}
