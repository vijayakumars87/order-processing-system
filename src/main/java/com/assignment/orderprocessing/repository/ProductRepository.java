package com.assignment.orderprocessing.repository;

import com.assignment.orderprocessing.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
