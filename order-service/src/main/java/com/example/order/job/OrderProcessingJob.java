package com.example.order.job;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProcessingJob {

    private final OrderRepository orderRepository;

    @Value("${order.processing.delay-ms:10000}")
    private long processingDelayMs;

    /**
     * Simulates a long-running background job for payment verification.
     * Runs asynchronously on a virtual thread (Java 21).
     *
     * In production, this would integrate with a payment gateway,
     * fraud detection service, or other external systems.
     */
    @Async
    public void processOrderAsync(Long orderId) {
        log.info("[BACKGROUND JOB] Starting async payment verification for order ID: {}", orderId);

        try {
            // Simulate long-running payment verification
            Thread.sleep(processingDelayMs);

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("[BACKGROUND JOB] Order not found: {}", orderId);
                return;
            }

            if (order.getStatus() != OrderStatus.PENDING) {
                log.info("[BACKGROUND JOB] Order {} is no longer PENDING (status: {}), skipping",
                        order.getOrderNumber(), order.getStatus());
                return;
            }

            // Simulate payment verification result (always succeeds in POC)
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            log.info("[BACKGROUND JOB] Payment verified. Order {} confirmed (total: {})",
                    order.getOrderNumber(), order.getTotalPrice());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[BACKGROUND JOB] Payment verification interrupted for order: {}", orderId);
        }
    }
}
