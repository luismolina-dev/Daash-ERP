package com.daash.erp.repository;

import com.daash.erp.model.ContingencyQueue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContingencyQueueRepository extends JpaRepository<ContingencyQueue, Long> {
    List<ContingencyQueue> findByEstado(String estado);

    List<ContingencyQueue> findBySaleId(Long saleId);

    Optional<ContingencyQueue> findFirstBySaleIdOrderByIdDesc(Long saleId);
}
