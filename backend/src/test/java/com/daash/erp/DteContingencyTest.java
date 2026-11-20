package com.daash.erp;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daash.erp.model.Role;
import com.daash.erp.model.Sale;
import com.daash.erp.model.User;
import com.daash.erp.service.DteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class DteContingencyTest extends AbstractIntegrationTest {
    @Autowired private DteService dteService;

    @Test
    void contingenciaLuegoReintentoAceptaYGuardaSello() throws Exception {
        User manager = createUser("gerente@test.com", Role.GERENTE);
        var product = createProduct("P-DTE", "Producto DTE", "50.00", 10);
        openRegister(manager);

        mockMvc.perform(post("/api/dte/simulation/downtime")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"down\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.down").value(true));

        String response = mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metodoPago":"efectivo","items":[{"productId":"%d","cantidad":1,"descuento":0}]}
                                """.formatted(product.id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoDTE").value("contingencia"))
                .andReturn().getResponse().getContentAsString();
        Long saleId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/dte/contingencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado").value("pendiente"))
                .andExpect(jsonPath("$[0].maxIntentos").value(5));

        mockMvc.perform(post("/api/dte/simulation/downtime")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"down\":false}"))
                .andExpect(status().isOk());

        dteService.retryPending(true);

        Sale sale = saleRepository.findById(saleId).orElseThrow();
        assertEquals("aceptado", sale.estadoDTE);
        assertNotNull(sale.uuid);
        assertEquals("enviado", contingencyQueueRepository.findAll().get(0).estado);
        org.junit.jupiter.api.Assertions.assertTrue(
                sale.dteJson.contains("\"version\":\"1.4\""));
        org.junit.jupiter.api.Assertions.assertNotNull(sale.dteFirma);
    }

    @Test
    void cajeroNoPuedeConsultarContingenciasNiCambiarDowntime() throws Exception {
        User cashier = createUser("cajero@test.com", Role.CAJERO);

        mockMvc.perform(get("/api/dte/contingencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/dte/simulation/downtime")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"down\":true}"))
                .andExpect(status().isForbidden());
    }
}
