package com.daash.erp.service;

import com.daash.erp.dto.InventoryDtos.KardexDto;
import com.daash.erp.repository.KardexRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KardexQueryService {
    private final KardexRepository kardexRepository;

    public KardexQueryService(KardexRepository kardexRepository) {
        this.kardexRepository = kardexRepository;
    }

    @Transactional(readOnly = true)
    public List<KardexDto> list(Long productId) {
        if (productId == null) {
            return kardexRepository.findAllByOrderByFechaDesc().stream().map(KardexDto::from).toList();
        }
        return kardexRepository.findByProductoIdOrderByFechaDesc(productId).stream()
                .map(KardexDto::from).toList();
    }
}
