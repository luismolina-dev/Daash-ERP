package com.daash.erp.repository;

import com.daash.erp.model.KardexEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KardexRepository extends JpaRepository<KardexEntry, Long> {
    List<KardexEntry> findByProductoIdOrderByFechaDesc(Long productoId);

    List<KardexEntry> findAllByOrderByFechaDesc();
}
