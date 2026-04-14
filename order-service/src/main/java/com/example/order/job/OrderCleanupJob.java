package com.example.order.job;

import com.example.order.client.InventoryClient;
import com.example.order.model.Order;
import com.example.order.model.OrderLineItem;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupJob {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    @Value("${order.cleanup.stale-minutes:30}")
    private int staleMinutes;

    /**
     * Scheduled background job that runs periodically to cancel stale orders.
     * Orders that remain in PENDING status beyond the configured threshold
     * are automatically cancelled and their inventory is restored.
     */
    @Scheduled(fixedRateString = "${order.cleanup.rate-ms:300000}")
    @Transactional
    public void cleanupStaleOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleMinutes);
        List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, threshold);

        if (staleOrders.isEmpty()) {
            log.debug("[SCHEDULED JOB] No stale orders found");
            return;
        }

        log.info("[SCHEDULED JOB] Found {} stale orders to cancel", staleOrders.size());

        for (Order order : staleOrders) {
            // Restore inventory for each line item
            for (OrderLineItem item : order.getLineItems()) {
                try {
                    inventoryClient.restoreStock(item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("[SCHEDULED JOB] Failed to restore stock for product {} in order {}",
                            item.getProductId(), order.getOrderNumber(), e);
                }
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("[SCHEDULED JOB] Cancelled stale order: {} (created: {})",
                    order.getOrderNumber(), order.getCreatedAt());
        }

        log.info("[SCHEDULED JOB] Cleanup complete. Cancelled {} stale orders", staleOrders.size());
    }
}
