package com.daash.erp.model;

import java.util.Locale;

public enum Role {
    CAJERO,
    ADMIN_BODEGA,
    GERENTE,
    AUDITOR;

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Role from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Rol no especificado");
        }
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
