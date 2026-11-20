package com.daash.erp;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daash.erp.model.Product;
import com.daash.erp.model.Role;
import com.daash.erp.model.Supplier;
import com.daash.erp.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.mail.host=")
class PurchaseInvoiceTest extends AbstractIntegrationTest {

    @Test
    void compraGeneraPdfYRegistraCorreoSimulado() throws Exception {
        User admin = createUser("carlos@test.com", Role.ADMIN_BODEGA);
        Product product = createProduct("P-COMPRA", "Teclado", "100.00", 10);
        Supplier supplier = new Supplier();
        supplier.nombre = "Proveedor SA";
        supplier.nit = "0614-999999-101-0";
        supplier.nrc = "9999-1";
        supplier.email = "proveedor@test.com";
        supplier.direccion = "San Salvador";
        supplier.contacto = "Juan";
        supplier.telefono = "22222222";
        supplier = supplierRepository.save(supplier);

        String response = mockMvc.perform(post("/api/purchases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "%d",
                                  "customerEmail": "cliente@test.com",
                                  "items": [{"productId": "%d", "cantidad": 2, "precioUnitario": 100.00}]
                                }
                                """.formatted(supplier.id, product.id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCompra").isNotEmpty())
                .andExpect(jsonPath("$.customerEmail").value("cliente@test.com"))
                .andExpect(jsonPath("$.emailEstado").value("SIMULADO"))
                .andExpect(jsonPath("$.pdfDisponible").value(true))
                .andExpect(jsonPath("$.subtotal").value(closeTo(200.0, 0.001)))
                .andExpect(jsonPath("$.iva").value(closeTo(26.0, 0.001)))
                .andExpect(jsonPath("$.total").value(closeTo(226.0, 0.001)))
                .andReturn().getResponse().getContentAsString();

        Long purchaseId = objectMapper.readTree(response).get("id").asLong();

        byte[] pdf = mockMvc.perform(get("/api/purchases/" + purchaseId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("inline")))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(pdf.length > 2000, "El PDF debe incluir el QR y los estilos");
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        assertTrue(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1).contains("/Image"),
                "El PDF debe incluir la imagen del QR");
        assertEquals(12, productRepository.findById(product.id).orElseThrow().stock);
    }

    @Test
    void cajeroNoPuedeVerComprasNiPdf() throws Exception {
        User cashier = createUser("cajero@test.com", Role.CAJERO);

        mockMvc.perform(get("/api/purchases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/purchases/1/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier)))
                .andExpect(status().isForbidden());
    }

    @Test
    void correoInvalidoEsRechazado() throws Exception {
        User admin = createUser("carlos@test.com", Role.ADMIN_BODEGA);
        Product product = createProduct("P-COMPRA-2", "Mouse", "50.00", 10);
        Supplier supplier = new Supplier();
        supplier.nombre = "Proveedor 2";
        supplier.nit = "0614-888888-101-0";
        supplier = supplierRepository.save(supplier);

        mockMvc.perform(post("/api/purchases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":"%d","customerEmail":"correo-invalido",
                                 "items":[{"productId":"%d","cantidad":1,"precioUnitario":50.00}]}
                                """.formatted(supplier.id, product.id)))
                .andExpect(status().isBadRequest());
    }
}
