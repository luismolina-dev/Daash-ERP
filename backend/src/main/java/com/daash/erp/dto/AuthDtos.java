package com.daash.erp.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(@NotBlank(message = "email es obligatorio") String email,
                               @NotBlank(message = "password es obligatorio") String password) { }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserDto user) { }
}
