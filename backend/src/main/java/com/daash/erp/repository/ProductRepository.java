package com.daash.erp.repository;

import com.daash.erp.model.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
