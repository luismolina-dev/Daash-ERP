package com.daash.erp.repository;

import com.daash.erp.model.DteDocument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DteDocumentRepository extends JpaRepository<DteDocument, Long> {
    Optional<DteDocument> findBySaleId(Long saleId);
}
