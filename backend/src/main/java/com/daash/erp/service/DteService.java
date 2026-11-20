package com.daash.erp.service;

import com.daash.erp.model.ContingencyQueue;
import com.daash.erp.model.DteDocument;
import com.daash.erp.model.Sale;
import com.daash.erp.model.SaleItem;
import com.daash.erp.repository.ContingencyQueueRepository;
import com.daash.erp.repository.DteDocumentRepository;
import com.daash.erp.repository.SaleRepository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DteService {
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final ObjectMapper objectMapper;
    private final MockHaciendaClient haciendaClient;
    private final SaleRepository saleRepository;
    private final DteDocumentRepository dteDocumentRepository;
    private final ContingencyQueueRepository contingencyQueueRepository;
    private final AuditService auditService;
    private final SecretKey signingKey;

    public DteService(ObjectMapper objectMapper, MockHaciendaClient haciendaClient,
                      SaleRepository saleRepository, DteDocumentRepository dteDocumentRepository,
                      ContingencyQueueRepository contingencyQueueRepository, AuditService auditService,
                      @Value("${dte.signing-secret}") String signingSecret) {
        this.objectMapper = objectMapper;
        this.haciendaClient = haciendaClient;
        this.saleRepository = saleRepository;
        this.dteDocumentRepository = dteDocumentRepository;
        this.contingencyQueueRepository = contingencyQueueRepository;
        this.auditService = auditService;
        this.signingKey = Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Genera el DTE v1.4, lo firma con HS256 y lo envía al cliente simulado. */
    @Transactional
    public void issueDte(Sale sale) {
        String codigoGeneracion = UUID.randomUUID().toString().toUpperCase();
        String json = buildJson(sale, codigoGeneracion);
        String firma = sign(json, codigoGeneracion, sale.fecha);

        sale.dteJson = json;
        sale.dteFirma = firma;

        DteDocument document = new DteDocument();
        document.saleId = sale.id;
        document.codigoGeneracion = codigoGeneracion;
        document.fechaEmision = sale.fecha;
        document.json = json;
        document.firma = firma;
        document.estado = "pendiente";
        document.createdAt = Instant.now();
        dteDocumentRepository.save(document);

        if (haciendaClient.submit(json)) {
            accept(sale, document);
            audit("DTE_ACEPTADO", sale, "Sello " + sale.uuid);
        } else {
            sale.estadoDTE = "contingencia";
            sale.contingencyReason = "Servicio de Hacienda no disponible";
            ContingencyQueue queue = new ContingencyQueue();
            queue.saleId = sale.id;
            queue.intentos = 0;
            queue.maxIntentos = MAX_ATTEMPTS;
            queue.estado = "pendiente";
            queue.proximoReintento = Instant.now().plus(RETRY_DELAY);
            queue.ultimoError = sale.contingencyReason;
            queue.createdAt = Instant.now();
            queue.updatedAt = Instant.now();
            contingencyQueueRepository.save(queue);
            audit("DTE_CONTINGENCIA", sale, "Encolada para reintento en 5 minutos");
        }
        saleRepository.save(sale);
    }

    /** Reintenta la cola. force=true ignora proximoReintento (útil para pruebas del job). */
    @Transactional
    public void retryPending(boolean force) {
        Instant now = Instant.now();
        List<ContingencyQueue> pending = contingencyQueueRepository.findByEstado("pendiente");
        for (ContingencyQueue queue : pending) {
            if (!force && queue.proximoReintento != null && queue.proximoReintento.isAfter(now)) {
                continue;
            }
            Sale sale = saleRepository.findById(queue.saleId).orElse(null);
            if (sale == null || sale.dteJson == null) {
                queue.estado = "fallido";
                queue.ultimoError = "No se encontró el DTE de la venta";
                queue.updatedAt = now;
                contingencyQueueRepository.save(queue);
                continue;
            }
            boolean accepted = haciendaClient.submit(sale.dteJson);
            if (accepted) {
                dteDocumentRepository.findBySaleId(sale.id).ifPresent(document -> accept(sale, document));
                if (sale.uuid == null) {
                    sale.uuid = UUID.randomUUID().toString();
                }
                sale.estadoDTE = "aceptado";
                sale.contingencyReason = null;
                saleRepository.save(sale);
                queue.estado = "enviado";
                queue.updatedAt = now;
                contingencyQueueRepository.save(queue);
                audit("DTE_ACEPTADO_REINTENTO", sale, "Sello " + sale.uuid);
            } else {
                queue.intentos = (queue.intentos == null ? 0 : queue.intentos) + 1;
                queue.ultimoError = "Servicio de Hacienda no disponible";
                queue.updatedAt = now;
                if (queue.intentos >= queue.maxIntentos) {
                    queue.estado = "fallido";
                    sale.estadoDTE = "rechazado";
                    sale.contingencyReason = "Agotados los 5 intentos de envío";
                    saleRepository.save(sale);
                    audit("DTE_FALLIDO", sale, "Agotados los " + queue.maxIntentos + " intentos");
                } else {
                    queue.proximoReintento = now.plus(RETRY_DELAY);
                    audit("DTE_REINTENTO_FALLIDO", sale, "Intento " + queue.intentos + " de " + queue.maxIntentos);
                }
                contingencyQueueRepository.save(queue);
            }
        }
    }

    private void accept(Sale sale, DteDocument document) {
        sale.estadoDTE = "aceptado";
        sale.contingencyReason = null;
        if (sale.uuid == null) {
            sale.uuid = UUID.randomUUID().toString();
        }
        document.estado = "aceptado";
        document.sello = sale.uuid;
        dteDocumentRepository.save(document);
    }

    private String buildJson(Sale sale, String codigoGeneracion) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("version", "1.4");
        root.put("codigoGeneracion", codigoGeneracion);
        root.put("fechaEmision", sale.fecha.toString());

        ObjectNode emisor = root.putObject("emisor");
        emisor.put("nit", "0614-123456-100-0");
        emisor.put("nombre", "Daash ERP & DTE");
        emisor.put("actividadEconomica", "Venta al por menor");

        ObjectNode receptor = root.putObject("receptor");
        receptor.put("nombre", sale.clienteNombre == null ? "Consumidor Final" : sale.clienteNombre);
        receptor.put("tipoContribuyente", sale.clienteId == null ? "consumidor_final" : "contribuyente");

        ArrayNode items = root.putArray("items");
        int number = 1;
        for (SaleItem item : sale.items) {
            ObjectNode node = items.addObject();
            node.put("numero", number++);
            node.put("codigo", item.productoCodigo);
            node.put("descripcion", item.productoNombre);
            node.put("cantidad", item.cantidad);
            node.put("precioUnitario", item.precioUnitario);
            node.put("descuento", item.descuento);
            node.put("subtotal", item.montoLinea);
        }
        root.put("subtotal", sale.subtotal);
        root.put("iva", sale.iva);
        root.put("total", sale.total);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar el DTE", ex);
        }
    }

    private String sign(String json, String codigoGeneracion, Instant fecha) {
        return Jwts.builder()
                .id(codigoGeneracion)
                .claim("dte", json)
                .issuedAt(Date.from(fecha))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private void audit(String action, Sale sale, String details) {
        auditService.log(action, "Sale", sale.id == null ? null : sale.id.toString(), sale.userId, details);
    }
}
