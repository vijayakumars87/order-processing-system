package com.assignment.orderprocessing.dto;

import com.assignment.orderprocessing.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, message = "password must be at least 6 characters")
        String password,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "fullName is required")
        String fullName,

        @NotBlank(message = "mobileNumber is required")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "mobileNumber must be 7-15 digits, optionally prefixed with +")
        String mobileNumber,

        /** Optional; defaults to CUSTOMER when null. */
        Role role
) {
}
