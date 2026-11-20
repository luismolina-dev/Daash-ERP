package com.daash.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "dte_documents", uniqueConstraints = @UniqueConstraint(name = "uk_dte_sale", columnNames = "sale_id"))
public class DteDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "sale_id", nullable = false)
    public Long saleId;

    @Column(name = "codigo_generacion", nullable = false, length = 60)
    public String codigoGeneracion;

    @Column(name = "fecha_emision", nullable = false)
    public Instant fechaEmision;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String json;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String firma;

    @Column(length = 20)
    public String estado = "pendiente";

    @Column(length = 60)
    public String sello;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
