package com.daash.erp.service;

import com.daash.erp.dto.InventoryDtos.KardexDto;
import com.daash.erp.model.KardexEntry;
import com.daash.erp.model.Product;
import com.daash.erp.repository.KardexRepository;
import com.daash.erp.repository.ProductRepository;
import com.daash.erp.web.BusinessException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final ProductRepository productRepository;
    private final KardexRepository kardexRepository;

    public InventoryService(ProductRepository productRepository, KardexRepository kardexRepository) {
        this.productRepository = productRepository;
        this.kardexRepository = kardexRepository;
    }

    /** Aplica un movimiento de stock y deja su traza en Kardex. */
    @Transactional
    public KardexDto move(Product product, int delta, String tipo, String justificacion,
                          Long userId, String referencia) {
        int anterior = product.stock == null ? 0 : product.stock;
        int posterior = anterior + delta;
        if (posterior < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente para " + product.nombre + " (disponible: " + anterior + ")");
        }
        product.stock = posterior;
        productRepository.save(product);

        KardexEntry entry = new KardexEntry();
        entry.fecha = Instant.now();
        entry.productoId = product.id;
        entry.productoNombre = product.nombre;
        entry.tipo = tipo;
        entry.cantidad = Math.abs(delta);
        entry.stockAnterior = anterior;
        entry.stockPosterior = posterior;
        entry.justificacion = justificacion;
        entry.userId = userId;
        entry.referencia = referencia;
        return KardexDto.from(kardexRepository.save(entry));
    }
}
