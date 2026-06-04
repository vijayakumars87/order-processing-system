package com.assignment.orderprocessing.repository;

import com.assignment.orderprocessing.domain.Order;
import com.assignment.orderprocessing.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomerUsername(String customerUsername);

    List<Order> findByCustomerUsernameAndStatus(String customerUsername, OrderStatus status);
}
