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
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_fecha", columnList = "fecha"),
        @Index(name = "idx_audit_accion", columnList = "accion")
})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public Instant fecha;

    @Column(nullable = false, length = 60)
    public String accion;

    @Column(length = 60)
    public String entidad;

    @Column(name = "entidad_id", length = 60)
    public String entidadId;

    @Column(name = "user_id")
    public Long userId;

    @Column(columnDefinition = "TEXT")
    public String detalles;

    @Column(length = 60)
    public String ip;
}
