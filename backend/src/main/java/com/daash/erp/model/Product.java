package com.daash.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_codigo", columnNames = "codigo"),
        indexes = @Index(name = "idx_products_categoria", columnList = "categoria"))
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 50)
    public String codigo;

    @Column(nullable = false, length = 160)
    public String nombre;

    @Column(length = 500)
    public String descripcion;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal precio;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal costo;

    @Column(nullable = false)
    public Integer stock = 0;

    @Column(name = "stock_minimo", nullable = false)
    public Integer stockMinimo = 0;

    @Column(nullable = false, length = 80)
    public String categoria;

    @Column(nullable = false)
    public boolean activo = true;
}
