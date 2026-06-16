package com.auth.dto.response;

import com.auth.entity.AuthProvider;
import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String profilePictureUrl,
    AuthProvider provider,
    boolean emailVerified,
    boolean enabled,
    Set<String> roles,        // ["USER", "ADMIN"]
    Set<String> permissions,  // ["user:read", "admin:write"]
    LocalDateTime createdAt
) {}
