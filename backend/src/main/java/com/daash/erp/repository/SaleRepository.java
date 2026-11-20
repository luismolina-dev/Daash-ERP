package com.daash.erp.repository;

import com.daash.erp.model.Sale;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findAllByOrderByFechaDesc();

    boolean existsByNumeroFactura(String numeroFactura);
}
