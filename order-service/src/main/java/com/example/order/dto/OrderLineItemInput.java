package com.example.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineItemInput(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity
) {
}
