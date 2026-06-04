package com.assignment.orderprocessing.service;

import com.assignment.orderprocessing.domain.Order;
import com.assignment.orderprocessing.domain.OrderItem;
import com.assignment.orderprocessing.domain.OrderStatus;
import com.assignment.orderprocessing.domain.Product;
import com.assignment.orderprocessing.dto.CreateOrderRequest;
import com.assignment.orderprocessing.dto.OrderItemRequest;
import com.assignment.orderprocessing.exception.InvalidOrderStateException;
import com.assignment.orderprocessing.exception.ResourceNotFoundException;
import com.assignment.orderprocessing.repository.OrderRepository;
import com.assignment.orderprocessing.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order createOrder(String customerUsername, CreateOrderRequest request) {
        Instant now = Instant.now();
        Order order = Order.builder()
                .customerUsername(customerUsername)
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        for (OrderItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + itemReq.productId()));

            if (product.getStock() < itemReq.quantity()) {
                throw new InvalidOrderStateException(
                        "Insufficient stock for product '" + product.getName()
                                + "': requested " + itemReq.quantity() + ", available " + product.getStock());
            }
            product.setStock(product.getStock() - itemReq.quantity());

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(product.getPrice())
                    .build();
            order.addItem(item);
        }

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> listOrders(OrderStatus status) {
        return status != null ? orderRepository.findByStatus(status) : orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> listOrdersForCustomer(String username, OrderStatus status) {
        return status != null
                ? orderRepository.findByCustomerUsernameAndStatus(username, status)
                : orderRepository.findByCustomerUsername(username);
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus target) {
        Order order = getOrder(id);
        if (!order.getStatus().canTransitionTo(target)) {
            throw new InvalidOrderStateException(
                    "Illegal status transition: " + order.getStatus() + " -> " + target);
        }
        order.setStatus(target);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long id) {
        Order order = getOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only PENDING orders can be cancelled; current status is " + order.getStatus());
        }
        // Return reserved stock to inventory.
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }
}
