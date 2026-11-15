package com.matriz.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/v1/auth/login}. Any other field is refused (see openapi.yaml). */
public record LoginRequest(
        @NotBlank(message = "Email is required.") @Size(max = 254, message = "Email is too long.") String email,
        @NotBlank(message = "Password is required.") @Size(max = 128, message = "Password is too long.") String password) {
}
