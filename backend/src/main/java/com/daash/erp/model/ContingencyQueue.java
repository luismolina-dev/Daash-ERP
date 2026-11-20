package com.daash.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "contingency_queue", indexes = @Index(name = "idx_contingency_estado_reintento", columnList = "estado, proximo_reintento"))
public class ContingencyQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "sale_id", nullable = false)
    public Long saleId;

    @Column(nullable = false)
    public Integer intentos = 0;

    @Column(name = "max_intentos", nullable = false)
    public Integer maxIntentos = 5;

    @Column(nullable = false, length = 20)
    public String estado = "pendiente";

    @Column(name = "proximo_reintento", nullable = false)
    public Instant proximoReintento;

    @Column(name = "ultimo_error", length = 500)
    public String ultimoError;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
