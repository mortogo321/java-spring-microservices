package com.example.order.controller;

import com.example.order.client.ProductClient;
import com.example.order.dto.OrderInput;
import com.example.order.dto.ProductResponse;
import com.example.order.job.OrderProcessingJob;
import com.example.order.model.Order;
import com.example.order.model.OrderLineItem;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderProcessingJob orderProcessingJob;
    private final ProductClient productClient;

    @QueryMapping
    public List<Order> orders() {
        return orderService.getAllOrders();
    }

    @QueryMapping
    public Order orderById(@Argument Long id) {
        return orderService.getOrderById(id);
    }

    @QueryMapping
    public List<Order> ordersByStatus(@Argument OrderStatus status) {
        return orderService.getOrdersByStatus(status);
    }

    @MutationMapping
    public Order placeOrder(@Argument OrderInput input) {
        Order order = orderService.placeOrder(input);
        // Trigger async background processing (payment verification simulation)
        orderProcessingJob.processOrderAsync(order.getId());
        return order;
    }

    @MutationMapping
    public Order cancelOrder(@Argument Long id) {
        return orderService.cancelOrder(id);
    }

    @BatchMapping(typeName = "OrderLineItem", field = "product")
    public Map<OrderLineItem, ProductResponse> products(List<OrderLineItem> lineItems) {
        List<Long> productIds = lineItems.stream()
                .map(OrderLineItem::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductResponse> productMap = productClient.getProductsByIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductResponse::id, p -> p));

        return lineItems.stream()
                .collect(Collectors.toMap(
                        li -> li,
                        li -> productMap.getOrDefault(li.getProductId(),
                                new ProductResponse(li.getProductId(), "Unknown", null))
                ));
    }
}
