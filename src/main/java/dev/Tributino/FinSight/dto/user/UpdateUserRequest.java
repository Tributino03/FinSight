package dev.Tributino.FinSight.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        String name,

        @Size(min = 8, message = "Password must contain at least 8 characters")
        String password
) {
}