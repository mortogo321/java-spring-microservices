package com.example.order.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderInput(
        @NotEmpty List<OrderLineItemInput> lineItems
) {
}
