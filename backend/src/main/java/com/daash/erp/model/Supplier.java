package com.daash.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers", indexes = @Index(name = "idx_suppliers_nit", columnList = "nit"))
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 160)
    public String nombre;

    @Column(length = 20)
    public String nit;

    @Column(length = 20)
    public String nrc;

    @Column(length = 30)
    public String telefono;

    @Column(length = 180)
    public String email;

    @Column(length = 300)
    public String direccion;

    @Column(length = 120)
    public String contacto;
}
