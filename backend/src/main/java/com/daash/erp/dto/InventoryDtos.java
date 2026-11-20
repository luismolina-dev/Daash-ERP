package com.daash.erp.dto;

import com.daash.erp.model.KardexEntry;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class InventoryDtos {
    private InventoryDtos() { }

    public record InventoryAdjustmentRequest(@NotNull Long productId,
                                              @NotBlank String tipo,
                                              @NotNull @Min(1) Integer cantidad,
                                              @NotBlank String justificacion) { }

    public record KardexDto(Long id, Instant fecha, Long productoId, String productoNombre, String tipo,
                             Integer cantidad, Integer stockAnterior, Integer stockPosterior,
                             String justificacion, Long userId, String referencia) {
        public static KardexDto from(KardexEntry entry) {
            return new KardexDto(entry.id, entry.fecha, entry.productoId, entry.productoNombre, entry.tipo,
                    entry.cantidad, entry.stockAnterior, entry.stockPosterior, entry.justificacion,
                    entry.userId, entry.referencia);
        }
    }

    public record PurchaseRequest(@NotNull Long supplierId,
                                   @NotBlank @Email String customerEmail,
                                   @NotNull java.util.List<Item> items) {
        public record Item(@NotNull Long productId,
                           @NotNull @Min(1) Integer cantidad,
                           @NotNull @DecimalMin("0.01") BigDecimal precioUnitario) { }
    }

    public record PurchaseItemDto(Long productoId, String productoNombre, Integer cantidad,
                                   BigDecimal precioUnitario, BigDecimal subtotal) { }

    public record PurchaseDto(Long id, String numeroCompra, Instant fecha,
                               Long proveedorId, String proveedorNombre,
                               String customerEmail, String emailEstado, Instant emailFecha, String emailError,
                               boolean pdfDisponible, String codigoGeneracion,
                               java.util.List<PurchaseItemDto> items,
                               BigDecimal subtotal, BigDecimal iva, BigDecimal total) { }
}
