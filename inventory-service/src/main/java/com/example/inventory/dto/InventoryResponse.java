package com.example.inventory.dto;

public record InventoryResponse(
        Long productId,
        Integer quantity,
        boolean inStock
) {
}
