package com.example.inventory.controller;

import com.example.inventory.dto.InventoryResponse;
import com.example.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> checkStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.checkStock(productId));
    }

    @PutMapping("/{productId}/decrement")
    public ResponseEntity<InventoryResponse> decrementStock(
            @PathVariable Long productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.decrementStock(productId, quantity));
    }

    @PutMapping("/{productId}/restore")
    public ResponseEntity<InventoryResponse> restoreStock(
            @PathVariable Long productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.restoreStock(productId, quantity));
    }
}
