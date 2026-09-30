package dev.Tributino.FinSight.dto.transaction;

import jakarta.validation.constraints.NotBlank;

public record TransactionUpdateRequest(

        @NotBlank(message = "Description is required")
        String newDescription
) {
}
