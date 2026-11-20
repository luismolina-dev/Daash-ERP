package com.daash.erp.dto;

import com.daash.erp.model.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class ProductDtos {
    private ProductDtos() { }

    public record ProductDto(Long id, String codigo, String nombre, String descripcion, BigDecimal precio,
                             BigDecimal costo, Integer stock, Integer stockMinimo, String categoria, Boolean activo) {
        public static ProductDto from(Product product) {
            return new ProductDto(product.id, product.codigo, product.nombre, product.descripcion, product.precio,
                    product.costo, product.stock, product.stockMinimo, product.categoria, product.activo);
        }
    }

    public record ProductCreateRequest(
            @NotBlank String codigo,
            @NotBlank String nombre,
            String descripcion,
            @NotBlank String categoria,
            @NotNull @DecimalMin("0.01") BigDecimal precio,
            @NotNull @DecimalMin("0.00") BigDecimal costo,
            @NotNull @Min(0) Integer stockMinimo,
            Boolean activo) { }

    public record ProductPatchRequest(String nombre, String descripcion, String categoria,
                                      @DecimalMin("0.01") BigDecimal precio,
                                      @DecimalMin("0.00") BigDecimal costo,
                                      @Min(0) Integer stockMinimo,
                                      Boolean activo) { }
}
