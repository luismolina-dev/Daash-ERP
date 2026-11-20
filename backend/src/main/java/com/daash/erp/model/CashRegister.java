package com.daash.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cash_registers", indexes = @Index(name = "idx_cash_registers_abierta", columnList = "abierta"))
public class CashRegister {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "fecha_apertura", nullable = false)
    public Instant fechaApertura;

    @Column(name = "fecha_cierre")
    public Instant fechaCierre;

    @Column(name = "fondo_inicial", nullable = false, precision = 14, scale = 2)
    public BigDecimal fondoInicial;

    @Column(name = "total_efectivo", nullable = false, precision = 14, scale = 2)
    public BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(name = "total_tarjeta", nullable = false, precision = 14, scale = 2)
    public BigDecimal totalTarjeta = BigDecimal.ZERO;

    @Column(name = "total_transferencia", nullable = false, precision = 14, scale = 2)
    public BigDecimal totalTransferencia = BigDecimal.ZERO;

    @Column(name = "total_ventas", nullable = false, precision = 14, scale = 2)
    public BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(nullable = false)
    public boolean abierta = true;

    @Column(name = "fecha_arqueo")
    public Instant fechaArqueo;

    @Column(name = "efectivo_esperado", precision = 14, scale = 2)
    public BigDecimal efectivoEsperado;

    @Column(name = "efectivo_real", precision = 14, scale = 2)
    public BigDecimal efectivoReal;

    @Column(name = "tarjeta_real", precision = 14, scale = 2)
    public BigDecimal tarjetaReal;

    @Column(name = "transferencia_real", precision = 14, scale = 2)
    public BigDecimal transferenciaReal;

    @Column(precision = 14, scale = 2)
    public BigDecimal diferencia;
}
