package com.pro.auth.Auth_app_backend.dtos;

import tools.jackson.core.base.BinaryTSFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        OffsetDateTime timestamp
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
