package com.daash.erp;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daash.erp.model.Role;
import com.daash.erp.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthSecurityTest extends AbstractIntegrationTest {

    @Test
    void loginCorrectoDevuelveTokenYCookie() throws Exception {
        User cashier = createUser("maria@adventureworks.com", Role.CAJERO);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + cashier.email + "\",\"password\":\"Demo1234!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.role").value("cajero"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
    }

    @Test
    void loginIncorrectoDevuelve401() throws Exception {
        User cashier = createUser("maria@adventureworks.com", Role.CAJERO);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + cashier.email + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitBloqueaTrasCincoFallos() throws Exception {
        User cashier = createUser("maria@adventureworks.com", Role.CAJERO);
        String body = "{\"email\":\"" + cashier.email + "\",\"password\":\"wrong\"}";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void cajeroNoPuedeVerReportesYGerenteSi() throws Exception {
        User cashier = createUser("cajero@test.com", Role.CAJERO);
        User manager = createUser("gerente@test.com", Role.GERENTE);

        mockMvc.perform(get("/api/reports/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentas").value(0));
    }

    @Test
    void tokenManipuladoEsRechazado() throws Exception {
        User cashier = createUser("cajero@test.com", Role.CAJERO);
        String token = token(cashier);
        String altered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + altered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cabecerasDeSeguridadEstanPresentes() throws Exception {
        var result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andReturn();

        assertNotNull(result.getResponse().getHeader("Content-Security-Policy"));
        org.junit.jupiter.api.Assertions.assertTrue(
                result.getResponse().getHeader("Content-Security-Policy").contains("default-src 'self'"));
        org.junit.jupiter.api.Assertions.assertEquals("DENY", result.getResponse().getHeader("X-Frame-Options"));
        org.junit.jupiter.api.Assertions.assertEquals("nosniff", result.getResponse().getHeader("X-Content-Type-Options"));
        org.junit.jupiter.api.Assertions.assertEquals("no-referrer", result.getResponse().getHeader("Referrer-Policy"));
        org.junit.jupiter.api.Assertions.assertTrue(
                result.getResponse().getHeader("Strict-Transport-Security").contains("max-age="));
    }

    @Test
    void refreshConCookieDevuelveNuevoAccessToken() throws Exception {
        User cashier = createUser("cajero@test.com", Role.CAJERO);
        String refresh = jwtService.generateRefreshToken(cashier);

        mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refreshToken", refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
