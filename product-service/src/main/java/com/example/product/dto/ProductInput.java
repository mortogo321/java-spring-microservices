package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductInput(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        @NotBlank String category
) {
}
