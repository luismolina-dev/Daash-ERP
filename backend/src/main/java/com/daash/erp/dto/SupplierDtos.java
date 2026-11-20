package com.daash.erp.dto;

import com.daash.erp.model.Supplier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class SupplierDtos {
    private SupplierDtos() { }

    public record SupplierDto(Long id, String nombre, String nit, String nrc, String telefono,
                              String email, String direccion, String contacto) {
        public static SupplierDto from(Supplier supplier) {
            return new SupplierDto(supplier.id, supplier.nombre, supplier.nit, supplier.nrc, supplier.telefono,
                    supplier.email, supplier.direccion, supplier.contacto);
        }
    }

    public record SupplierCreateRequest(
            @NotBlank String nombre,
            @Pattern(regexp = "^\\d{4}-\\d{6}-\\d{3}-\\d$", message = "NIT debe tener formato 0614-123456-100-0") String nit,
            String nrc,
            String telefono,
            @Email String email,
            String direccion,
            String contacto) { }
}
