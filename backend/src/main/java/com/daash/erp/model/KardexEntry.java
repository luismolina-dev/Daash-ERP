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
@Table(name = "kardex_entries", indexes = {
        @Index(name = "idx_kardex_producto_fecha", columnList = "producto_id, fecha"),
        @Index(name = "idx_kardex_tipo", columnList = "tipo")
})
public class KardexEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public Instant fecha;

    @Column(name = "producto_id", nullable = false)
    public Long productoId;

    @Column(name = "producto_nombre", length = 160)
    public String productoNombre;

    @Column(nullable = false, length = 30)
    public String tipo;

    @Column(nullable = false)
    public Integer cantidad;

    @Column(name = "stock_anterior", nullable = false)
    public Integer stockAnterior;

    @Column(name = "stock_posterior", nullable = false)
    public Integer stockPosterior;

    @Column(length = 500)
    public String justificacion;

    @Column(name = "user_id")
    public Long userId;

    @Column(length = 120)
    public String referencia;
}
