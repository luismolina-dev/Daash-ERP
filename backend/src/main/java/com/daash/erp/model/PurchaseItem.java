package com.daash.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_items")
public class PurchaseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    public Purchase purchase;

    @Column(name = "producto_id", nullable = false)
    public Long productoId;

    @Column(name = "producto_nombre", length = 160)
    public String productoNombre;

    @Column(nullable = false)
    public Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    public BigDecimal precioUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal subtotal;
}
