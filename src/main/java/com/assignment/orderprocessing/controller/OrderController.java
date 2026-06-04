package com.assignment.orderprocessing.controller;

import com.assignment.orderprocessing.domain.Order;
import com.assignment.orderprocessing.domain.OrderStatus;
import com.assignment.orderprocessing.dto.CreateOrderRequest;
import com.assignment.orderprocessing.dto.OrderMapper;
import com.assignment.orderprocessing.dto.OrderResponse;
import com.assignment.orderprocessing.dto.UpdateOrderStatusRequest;
import com.assignment.orderprocessing.security.SecurityUtil;
import com.assignment.orderprocessing.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Create, retrieve, list, update and cancel orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place a new order with one or more items")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(SecurityUtil.currentUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderMapper.toResponse(order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve order details by ID (owner or ADMIN only)")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        Order order = orderService.getOrder(id);
        assertOwnerOrAdmin(order.getCustomerUsername());
        return ResponseEntity.ok(OrderMapper.toResponse(order));
    }

    @GetMapping
    @Operation(summary = "List orders, optionally filtered by status. "
            + "ADMIN sees all orders; CUSTOMER sees only their own.")
    public ResponseEntity<List<OrderResponse>> list(@RequestParam(required = false) OrderStatus status) {
        List<Order> orders = SecurityUtil.isAdmin()
                ? orderService.listOrders(status)
                : orderService.listOrdersForCustomer(SecurityUtil.currentUsername(), status);
        return ResponseEntity.ok(orders.stream().map(OrderMapper::toResponse).toList());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status following the lifecycle state machine (ADMIN only)")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order order = orderService.updateStatus(id, request.status());
        return ResponseEntity.ok(OrderMapper.toResponse(order));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (only allowed while PENDING; owner or ADMIN)")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        Order existing = orderService.getOrder(id);
        assertOwnerOrAdmin(existing.getCustomerUsername());
        Order cancelled = orderService.cancelOrder(id);
        return ResponseEntity.ok(OrderMapper.toResponse(cancelled));
    }

    private void assertOwnerOrAdmin(String ownerUsername) {
        if (!SecurityUtil.isAdmin() && !ownerUsername.equals(SecurityUtil.currentUsername())) {
            throw new AccessDeniedException("You do not have access to this order");
        }
    }
}
