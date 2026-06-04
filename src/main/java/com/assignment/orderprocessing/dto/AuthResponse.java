package com.assignment.orderprocessing.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String role
) {
}
