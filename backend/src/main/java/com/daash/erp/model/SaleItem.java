package com.daash.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_items", indexes = @Index(name = "idx_sale_items_producto", columnList = "producto_id"))
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    public Sale sale;

    @Column(name = "producto_id", nullable = false)
    public Long productoId;

    @Column(name = "producto_codigo", length = 50)
    public String productoCodigo;

    @Column(name = "producto_nombre", length = 160)
    public String productoNombre;

    @Column(nullable = false)
    public Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    public BigDecimal precioUnitario;

    @Column(nullable = false, precision = 5, scale = 2)
    public BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "monto_linea", nullable = false, precision = 14, scale = 2)
    public BigDecimal montoLinea;
}
