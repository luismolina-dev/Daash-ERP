package com.daash.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() { }

    public record TopProductDto(Long productoId, String nombre, Integer cantidad, BigDecimal monto) { }

    public record VentasPorDiaDto(LocalDate fecha, BigDecimal total) { }

    public record SummaryDto(BigDecimal totalIngresos, BigDecimal totalIVA, long totalVentas,
                             long lowStockProductCount, List<TopProductDto> topProducts,
                             List<VentasPorDiaDto> ventasPorDia) { }
}
