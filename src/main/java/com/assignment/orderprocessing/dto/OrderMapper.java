package com.assignment.orderprocessing.dto;

import com.assignment.orderprocessing.domain.Order;
import com.assignment.orderprocessing.domain.OrderItem;
import com.assignment.orderprocessing.domain.Product;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerUsername(),
                order.getStatus(),
                order.getItems().stream().map(OrderMapper::toItemResponse).toList(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        Product product = item.getProduct();
        return new OrderItemResponse(
                product.getId(),
                product.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
    }
}
