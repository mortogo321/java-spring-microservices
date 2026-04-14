package com.example.order.client;

import com.example.order.dto.InventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    public InventoryResponse checkStock(Long productId) {
        log.debug("Checking inventory for product: {}", productId);
        return webClientBuilder.build()
                .get()
                .uri("http://inventory-service/api/inventory/{productId}", productId)
                .retrieve()
                .bodyToMono(InventoryResponse.class)
                .block();
    }

    public InventoryResponse decrementStock(Long productId, int quantity) {
        log.debug("Decrementing inventory for product: {} by {}", productId, quantity);
        return webClientBuilder.build()
                .put()
                .uri("http://inventory-service/api/inventory/{productId}/decrement?quantity={quantity}",
                        productId, quantity)
                .retrieve()
                .bodyToMono(InventoryResponse.class)
                .block();
    }

    public InventoryResponse restoreStock(Long productId, int quantity) {
        log.debug("Restoring inventory for product: {} by {}", productId, quantity);
        return webClientBuilder.build()
                .put()
                .uri("http://inventory-service/api/inventory/{productId}/restore?quantity={quantity}",
                        productId, quantity)
                .retrieve()
                .bodyToMono(InventoryResponse.class)
                .block();
    }
}
