package com.daash.erp;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daash.erp.model.Product;
import com.daash.erp.model.Role;
import com.daash.erp.model.User;
import com.daash.erp.repository.SaleRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class SalesBusinessRulesTest extends AbstractIntegrationTest {

    @Test
    void precioAlteradoEsRecalculadoYStockDescendido() throws Exception {
        User cashier = createUser("cajero@test.com", Role.CAJERO);
        Product product = createProduct("P-1", "Bicicleta", "100.00", 10);
        openRegister(cashier);

        mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": null,
                                  "metodoPago": "efectivo",
                                  "items": [
                                    {"productId": "%d", "cantidad": 2, "descuento": 0,
                                     "precioUnitario": 1.00, "subtotal": 2.00, "iva": 0.26, "total": 2.26}
                                  ]
                                }
                                """.formatted(product.id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].precioUnitario").value(closeTo(100.0, 0.001)))
                .andExpect(jsonPath("$.subtotal").value(closeTo(200.0, 0.001)))
                .andExpect(jsonPath("$.iva").value(closeTo(26.0, 0.001)))
                .andExpect(jsonPath("$.total").value(closeTo(226.0, 0.001)))
                .andExpect(jsonPath("$.estadoDTE").value("aceptado"));

        Product updated = productRepository.findById(product.id).orElseThrow();
        assertEquals(0, new BigDecimal("8").compareTo(BigDecimal.valueOf(updated.stock)));
        assertEquals(1, kardexRepository.count());
    }

    @Test
    void descuentoMayorAl10SoloGerente() throws Exception {
        User cashier = createUser("cajero@test.com", Role.CAJERO);
        User manager = createUser("gerente@test.com", Role.GERENTE);
        Product product = createProduct("P-2", "Casco", "80.00", 20);
        openRegister(cashier);

        String body = """
                {"metodoPago":"tarjeta","items":[{"productId":"%d","cantidad":1,"descuento":15}]}
                """.formatted(product.id);

        mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(closeTo(76.84, 0.001)));
    }

    @Test
    void anularVentaSoloGerenteRestauraStock() throws Exception {
        User cashier = createUser("cajero@test.com", Role.CAJERO);
        User manager = createUser("gerente@test.com", Role.GERENTE);
        Product product = createProduct("P-3", "Luces", "20.00", 5);
        openRegister(cashier);

        String createBody = """
                {"metodoPago":"efectivo","items":[{"productId":"%d","cantidad":2,"descuento":0}]}
                """.formatted(product.id);
        String response = mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long saleId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/sales/" + saleId + "/anular")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/sales/" + saleId + "/anular")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulada").value(true))
                .andExpect(jsonPath("$.notaCreditoId").isNotEmpty());

        assertEquals(5, productRepository.findById(product.id).orElseThrow().stock);
        assertEquals(true, saleRepository.findById(saleId).orElseThrow().anulada);
    }
}
