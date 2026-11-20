package com.daash.erp.service;

import com.daash.erp.dto.ReportDtos.SummaryDto;
import com.daash.erp.dto.ReportDtos.TopProductDto;
import com.daash.erp.dto.ReportDtos.VentasPorDiaDto;
import com.daash.erp.model.Product;
import com.daash.erp.model.Purchase;
import com.daash.erp.model.Sale;
import com.daash.erp.model.SaleItem;
import com.daash.erp.repository.ProductRepository;
import com.daash.erp.repository.PurchaseRepository;
import com.daash.erp.repository.SaleRepository;
import com.daash.erp.web.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private static final BigDecimal IVA_RATE = new BigDecimal("0.13");

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;

    public ReportService(SaleRepository saleRepository, ProductRepository productRepository,
                         PurchaseRepository purchaseRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public SummaryDto summary() {
        List<Sale> sales = saleRepository.findAll().stream().filter(sale -> !sale.anulada).toList();
        BigDecimal totalIngresos = sales.stream().map(sale -> sale.total)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalIva = sales.stream().map(sale -> sale.iva)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        long lowStock = productRepository.findAll().stream()
                .filter(product -> product.stock <= product.stockMinimo).count();

        Map<Long, TopProductDto> totals = new LinkedHashMap<>();
        for (Sale sale : sales) {
            for (SaleItem item : sale.items) {
                totals.merge(item.productoId,
                        new TopProductDto(item.productoId, item.productoNombre, item.cantidad, item.montoLinea),
                        (a, b) -> new TopProductDto(a.productoId(), a.nombre(),
                                a.cantidad() + b.cantidad(), a.monto().add(b.monto())));
            }
        }
        List<TopProductDto> topProducts = totals.values().stream()
                .sorted(Comparator.comparing(TopProductDto::monto).reversed())
                .limit(10)
                .toList();

        Map<LocalDate, BigDecimal> byDay = sales.stream().collect(Collectors.groupingBy(
                sale -> sale.fecha.atZone(ZoneOffset.UTC).toLocalDate(),
                TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, sale -> sale.total, BigDecimal::add)));
        List<VentasPorDiaDto> ventasPorDia = byDay.entrySet().stream()
                .map(entry -> new VentasPorDiaDto(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        return new SummaryDto(totalIngresos, totalIva, sales.size(), lowStock, topProducts, ventasPorDia);
    }

    @Transactional(readOnly = true)
    public String ivaCsv(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "tipo es obligatorio");
        }
        return switch (tipo) {
            case "ventas_consumidor", "ventas_contribuyente" -> salesCsv(tipo);
            case "compras" -> purchasesCsv();
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "tipo de reporte IVA inválido");
        };
    }

    private String salesCsv(String tipo) {
        boolean contribuyente = "ventas_contribuyente".equals(tipo);
        StringBuilder csv = new StringBuilder("numeroFactura,fecha,cliente,subtotal,iva,total\n");
        for (Sale sale : saleRepository.findAllByOrderByFechaDesc()) {
            if (sale.anulada) continue;
            boolean saleIsContribuyente = sale.clienteId != null;
            if (saleIsContribuyente != contribuyente) continue;
            csv.append(escape(sale.numeroFactura)).append(',')
                    .append(escape(sale.fecha.toString())).append(',')
                    .append(escape(sale.clienteNombre)).append(',')
                    .append(sale.subtotal).append(',')
                    .append(sale.iva).append(',')
                    .append(sale.total).append('\n');
        }
        return csv.toString();
    }

    private String purchasesCsv() {
        StringBuilder csv = new StringBuilder("fecha,proveedor,producto,cantidad,precioUnitario,subtotal,iva,total\n");
        for (Purchase purchase : purchaseRepository.findAll()) {
            for (var item : purchase.items) {
                BigDecimal iva = item.subtotal.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = item.subtotal.add(iva);
                csv.append(escape(purchase.fecha.toString())).append(',')
                        .append(escape(purchase.supplierNombre)).append(',')
                        .append(escape(item.productoNombre)).append(',')
                        .append(item.cantidad).append(',')
                        .append(item.precioUnitario).append(',')
                        .append(item.subtotal).append(',')
                        .append(iva).append(',')
                        .append(total).append('\n');
            }
        }
        return csv.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
