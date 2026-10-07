package dev.Tributino.FinSight.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransactionUpdateRequest(

        @NotBlank(message = "Description is required")
        @Size(min = 5, max = 255, message = "Description must be between 5 and 255 characters")
        String newDescription
) {
}
