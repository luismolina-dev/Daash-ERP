package com.daash.erp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.daash.erp.model.Sale;
import com.daash.erp.model.SaleItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SaleDtos {
    private SaleDtos() { }

    public record SaleItemDto(Long productId, String nombre, Integer cantidad, BigDecimal precioUnitario,
                              BigDecimal descuento, BigDecimal subtotal) {
        public static SaleItemDto from(SaleItem item) {
            return new SaleItemDto(item.productoId, item.productoNombre, item.cantidad,
                    item.precioUnitario, item.descuento, item.montoLinea);
        }
    }

    public record SaleDto(Long id, String numeroFactura, Instant fecha, Long clienteId, String clienteNombre,
                          List<SaleItemDto> items, BigDecimal subtotal, BigDecimal iva, BigDecimal total,
                          String metodoPago, String estadoDTE, String uuid, String contingencyReason,
                          Long userId, Boolean anulada, String notaCreditoId, String dteJson, String dteFirma) {
        public static SaleDto from(Sale sale) {
            List<SaleItemDto> items = sale.items.stream().map(SaleItemDto::from).toList();
            return new SaleDto(sale.id, sale.numeroFactura, sale.fecha, sale.clienteId, sale.clienteNombre,
                    items, sale.subtotal, sale.iva, sale.total, sale.metodoPago, sale.estadoDTE, sale.uuid,
                    sale.contingencyReason, sale.userId, sale.anulada, sale.notaCreditoId,
                    sale.dteJson, sale.dteFirma);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SaleItemRequest(@NotBlank String productId,
                                  @NotNull @Min(1) Integer cantidad,
                                  @DecimalMin("0") @DecimalMax("100") BigDecimal descuento) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SaleCreateRequest(String customerId,
                                    @NotBlank @Pattern(regexp = "efectivo|tarjeta|transferencia", message = "metodoPago inválido") String metodoPago,
                                    @NotEmpty @Valid List<SaleItemRequest> items) { }
}
