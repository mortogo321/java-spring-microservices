package com.example.order.dto;

public record InventoryResponse(
        Long productId,
        Integer quantity,
        boolean inStock
) {
}
