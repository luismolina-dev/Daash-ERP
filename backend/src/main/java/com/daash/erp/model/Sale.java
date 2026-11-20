package com.daash.erp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales", indexes = {
        @Index(name = "idx_sales_fecha", columnList = "fecha"),
        @Index(name = "idx_sales_estado_dte", columnList = "estado_dte")
})
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 40)
    public String numeroFactura;

    @Column(nullable = false)
    public Instant fecha;

    @Column(name = "cliente_id")
    public Long clienteId;

    @Column(name = "cliente_nombre")
    public String clienteNombre;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<SaleItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal iva;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal total;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    public String metodoPago;

    @Column(name = "estado_dte", nullable = false, length = 20)
    public String estadoDTE;

    @Column(length = 60)
    public String uuid;

    @Column(name = "contingency_reason", length = 500)
    public String contingencyReason;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public boolean anulada = false;

    @Column(name = "nota_credito_id", length = 60)
    public String notaCreditoId;

    @Column(name = "dte_json", columnDefinition = "TEXT")
    public String dteJson;

    @Column(name = "dte_firma", columnDefinition = "TEXT")
    public String dteFirma;
}
