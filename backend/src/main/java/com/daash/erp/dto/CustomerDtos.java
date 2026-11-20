package com.daash.erp.dto;

import com.daash.erp.model.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class CustomerDtos {
    private CustomerDtos() { }

    public record CustomerDto(Long id, String nombre, String email, String telefono, String dui, String nit,
                              String nrc, String direccion, String tipoContribuyente) {
        public static CustomerDto from(Customer customer) {
            return new CustomerDto(customer.id, customer.nombre, customer.email, customer.telefono,
                    customer.dui, customer.nit, customer.nrc, customer.direccion, customer.tipoContribuyente);
        }
    }

    public record CustomerCreateRequest(
            @NotBlank String nombre,
            @Email String email,
            String telefono,
            @Pattern(regexp = "^\\d{8}-\\d$", message = "DUI debe tener formato 00000000-0") String dui,
            @Pattern(regexp = "^\\d{4}-\\d{6}-\\d{3}-\\d$", message = "NIT debe tener formato 0614-123456-100-0") String nit,
            String nrc,
            String direccion,
            String tipoContribuyente) { }
}
