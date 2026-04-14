package com.example.order.service;

import com.example.order.client.InventoryClient;
import com.example.order.client.ProductClient;
import com.example.order.dto.InventoryResponse;
import com.example.order.dto.OrderInput;
import com.example.order.dto.OrderLineItemInput;
import com.example.order.dto.ProductResponse;
import com.example.order.model.Order;
import com.example.order.model.OrderLineItem;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional
    public Order placeOrder(OrderInput input) {
        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.ZERO)
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderLineItemInput itemInput : input.lineItems()) {
            // Fetch product details from product-service (Feign)
            ProductResponse product = productClient.getProductById(itemInput.productId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + itemInput.productId());
            }

            // Check inventory via inventory-service (WebClient)
            InventoryResponse inventory = inventoryClient.checkStock(itemInput.productId());
            if (!inventory.inStock() || inventory.quantity() < itemInput.quantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.name()
                        + " (available: " + inventory.quantity() + ", requested: " + itemInput.quantity() + ")");
            }

            BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(itemInput.quantity()));

            OrderLineItem lineItem = OrderLineItem.builder()
                    .productId(itemInput.productId())
                    .quantity(itemInput.quantity())
                    .price(lineTotal)
                    .build();

            order.addLineItem(lineItem);
            totalPrice = totalPrice.add(lineTotal);
        }

        order.setTotalPrice(totalPrice);
        Order savedOrder = orderRepository.save(order);

        // Decrement inventory for all items
        for (OrderLineItemInput itemInput : input.lineItems()) {
            inventoryClient.decrementStock(itemInput.productId(), itemInput.quantity());
        }

        log.info("Order placed: {} with {} items, total: {}",
                savedOrder.getOrderNumber(), savedOrder.getLineItems().size(), savedOrder.getTotalPrice());

        return savedOrder;
    }

    @Transactional
    public Order cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order already cancelled: " + order.getOrderNumber());
        }

        // Restore inventory
        for (OrderLineItem item : order.getLineItems()) {
            inventoryClient.restoreStock(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        log.info("Order cancelled: {}", savedOrder.getOrderNumber());
        return savedOrder;
    }

    @Transactional
    public Order confirmOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be confirmed: " + order.getOrderNumber());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }
}
