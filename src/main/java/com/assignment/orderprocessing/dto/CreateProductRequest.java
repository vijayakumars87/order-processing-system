package com.assignment.orderprocessing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0")
        BigDecimal price,

        @PositiveOrZero(message = "stock cannot be negative")
        int stock
) {
}
