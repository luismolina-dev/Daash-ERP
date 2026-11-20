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
@Table(name = "purchases", indexes = @Index(name = "idx_purchases_fecha", columnList = "fecha"))
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "numero_compra", length = 32)
    public String numeroCompra;

    @Column(name = "supplier_id", nullable = false)
    public Long supplierId;

    @Column(name = "supplier_nombre", length = 160)
    public String supplierNombre;

    @Column(name = "supplier_nit", length = 20)
    public String supplierNit;

    @Column(name = "supplier_nrc", length = 20)
    public String supplierNrc;

    @Column(name = "supplier_direccion", length = 300)
    public String supplierDireccion;

    @Column(nullable = false)
    public Instant fecha;

    // Nullable en DB para permitir `ddl-auto=update` sobre compras anteriores al PDF.
    // El servicio siempre calcula estos valores al registrar una compra nueva.
    @Column(precision = 14, scale = 2)
    public BigDecimal subtotal;

    @Column(precision = 14, scale = 2)
    public BigDecimal iva;

    @Column(precision = 14, scale = 2)
    public BigDecimal total;

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "customer_email", length = 180)
    public String customerEmail;

    @Column(name = "email_estado", length = 20)
    public String emailEstado;

    @Column(name = "email_fecha")
    public Instant emailFecha;

    @Column(name = "email_error", length = 500)
    public String emailError;

    @Column(name = "codigo_generacion", length = 40)
    public String codigoGeneracion;

    @Column(name = "dte_json", columnDefinition = "text")
    public String dteJson;

    @Column(name = "dte_firma", columnDefinition = "text")
    public String dteFirma;

    /** PDF de la factura de compra; se guarda en la base para poder verla y descargarla después. */
    /** PDF en Base64 para persistirlo como texto portable (PostgreSQL y H2). */
    @Column(name = "pdf", columnDefinition = "text")
    public String pdf;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<PurchaseItem> items = new ArrayList<>();
}
