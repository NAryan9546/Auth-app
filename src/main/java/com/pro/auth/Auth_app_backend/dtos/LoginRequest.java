package com.pro.auth.Auth_app_backend.dtos;

public record LoginRequest(
        String email,
        String password
) {
}
