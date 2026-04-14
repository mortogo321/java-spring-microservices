package com.example.inventory.service;

import com.example.inventory.dto.InventoryResponse;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventoryResponse checkStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(inv -> new InventoryResponse(inv.getProductId(), inv.getQuantity(), inv.getQuantity() > 0))
                .orElse(new InventoryResponse(productId, 0, false));
    }

    @Transactional
    public InventoryResponse decrementStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("No inventory for product: " + productId));

        if (inventory.getQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + productId
                    + " (available: " + inventory.getQuantity() + ", requested: " + quantity + ")");
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);

        return new InventoryResponse(productId, inventory.getQuantity(), inventory.getQuantity() > 0);
    }

    @Transactional
    public InventoryResponse restoreStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("No inventory for product: " + productId));

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);

        return new InventoryResponse(productId, inventory.getQuantity(), true);
    }
}
