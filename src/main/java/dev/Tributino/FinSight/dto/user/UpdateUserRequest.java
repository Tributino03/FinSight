package dev.Tributino.FinSight.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(min = 5, max = 100,
                message = "Name must be between 5 and 100 characters")
        String name,

        @Size(min = 8,
                message = "Password must contain at least 8 characters")
        String password
) {
}