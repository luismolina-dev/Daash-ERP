package com.daash.erp.service;

import com.daash.erp.dto.InventoryDtos.PurchaseDto;
import com.daash.erp.dto.InventoryDtos.PurchaseItemDto;
import com.daash.erp.dto.InventoryDtos.PurchaseRequest;
import com.daash.erp.model.Product;
import com.daash.erp.model.Purchase;
import com.daash.erp.model.Supplier;
import com.daash.erp.repository.PurchaseRepository;
import com.daash.erp.repository.UserRepository;
import com.daash.erp.web.AuthContext;
import com.daash.erp.web.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class PurchaseService {
    private static final BigDecimal IVA_RATE = new BigDecimal("0.13");

    private final PurchaseRepository purchaseRepository;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PurchasePdfService pdfService;
    private final PurchaseMailService mailService;
    private final ObjectMapper objectMapper;
    private final SecretKey signingKey;
    private final String companyNombre;
    private final String companyNit;

    public PurchaseService(PurchaseRepository purchaseRepository, SupplierService supplierService,
                           ProductService productService, InventoryService inventoryService,
                           UserRepository userRepository, AuditService auditService,
                           PurchasePdfService pdfService, PurchaseMailService mailService,
                           ObjectMapper objectMapper,
                           @Value("${dte.signing-secret}") String signingSecret,
                           @Value("${app.company.nombre}") String companyNombre,
                           @Value("${app.company.nit}") String companyNit) {
        this.purchaseRepository = purchaseRepository;
        this.supplierService = supplierService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.pdfService = pdfService;
        this.mailService = mailService;
        this.objectMapper = objectMapper;
        this.signingKey = Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8));
        this.companyNombre = companyNombre;
        this.companyNit = companyNit;
    }

    @Transactional
    public PurchaseDto create(PurchaseRequest request) {
        Supplier supplier = supplierService.findById(request.supplierId());
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La compra debe incluir al menos un ítem");
        }
        Long userId = currentUserId();
        Purchase purchase = new Purchase();
        purchase.supplierId = supplier.id;
        purchase.supplierNombre = supplier.nombre;
        purchase.supplierNit = supplier.nit;
        purchase.supplierNrc = supplier.nrc;
        purchase.supplierDireccion = supplier.direccion;
        purchase.fecha = Instant.now();
        purchase.userId = userId;
        purchase.customerEmail = request.customerEmail();
        purchase.codigoGeneracion = UUID.randomUUID().toString().toUpperCase();
        purchase.emailEstado = "PENDIENTE";
        List<PurchaseItemDto> itemDtos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (PurchaseRequest.Item item : request.items()) {
            Product product = productService.findById(item.productId());
            BigDecimal lineSubtotal = item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad()));
            inventoryService.move(product, item.cantidad(), "compra",
                    "Compra a " + supplier.nombre, userId, "COMPRA-" + supplier.id);
            var persisted = new com.daash.erp.model.PurchaseItem();
            persisted.purchase = purchase;
            persisted.productoId = product.id;
            persisted.productoNombre = product.nombre;
            persisted.cantidad = item.cantidad();
            persisted.precioUnitario = item.precioUnitario();
            persisted.subtotal = lineSubtotal;
            purchase.items.add(persisted);
            itemDtos.add(new PurchaseItemDto(product.id, product.nombre, item.cantidad(),
                    item.precioUnitario(), lineSubtotal));
            subtotal = subtotal.add(lineSubtotal);
        }

        purchase.subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        purchase.iva = purchase.subtotal.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        purchase.total = purchase.subtotal.add(purchase.iva);
        String json = buildDteJson(purchase);
        purchase.dteJson = json;
        purchase.dteFirma = sign(json, purchase.codigoGeneracion, purchase.fecha);

        Purchase saved = purchaseRepository.save(purchase);
        saved.numeroCompra = String.format("CF-%06d", saved.id);
        try {
            saved.pdf = Base64.getEncoder().encodeToString(pdfService.generate(saved));
        } catch (Exception ex) {
            saved.emailEstado = "FALLIDO";
            saved.emailError = "No se pudo generar el PDF: " + truncate(ex.getMessage());
        }

        if (hasPdf(saved)) {
            PurchaseMailService.MailResult result = mailService.sendInvoice(
                    saved.customerEmail, saved.numeroCompra, pdfBytes(saved));
            saved.emailEstado = result.estado();
            saved.emailError = result.error();
            saved.emailFecha = Instant.now();
        }

        saved = purchaseRepository.save(saved);
        auditService.log("COMPRA", "Purchase", saved.id.toString(), userId,
                "Compra a " + supplier.nombre + " por " + saved.total
                        + "; correo=" + saved.emailEstado + "; pdf=" + hasPdf(saved));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PurchaseDto> list() {
        return purchaseRepository.findAll(Sort.by(Sort.Direction.DESC, "fecha"))
                .stream().map(PurchaseService::toDto).toList();
    }

    @Transactional
    public byte[] pdf(Long id) {
        Purchase purchase = find(id);
        // Se regenera en cada consulta para que compras anteriores usen el diseño/QR vigente.
        purchase.pdf = Base64.getEncoder().encodeToString(pdfService.generate(purchase));
        purchase = purchaseRepository.save(purchase);
        return pdfBytes(purchase);
    }

    @Transactional
    public PurchaseDto resendEmail(Long id) {
        Purchase purchase = find(id);
        purchase.pdf = Base64.getEncoder().encodeToString(pdfService.generate(purchase));
        PurchaseMailService.MailResult result = mailService.sendInvoice(
                purchase.customerEmail, purchase.numeroCompra, pdfBytes(purchase));
        purchase.emailEstado = result.estado();
        purchase.emailError = result.error();
        purchase.emailFecha = Instant.now();
        purchase = purchaseRepository.save(purchase);
        auditService.log("COMPRA_CORREO_REENVIADO", "Purchase", purchase.id.toString(), purchase.userId,
                "Correo=" + purchase.emailEstado);
        return toDto(purchase);
    }

    private Purchase find(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
    }

    private boolean hasPdf(Purchase purchase) {
        return purchase.pdf != null && !purchase.pdf.isBlank();
    }

    private byte[] pdfBytes(Purchase purchase) {
        return Base64.getDecoder().decode(purchase.pdf);
    }

    private Long currentUserId() {
        String email = AuthContext.currentEmail();
        if (email == null) return null;
        return userRepository.findByEmail(email).map(user -> user.id).orElse(null);
    }

    private String buildDteJson(Purchase purchase) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("version", "1.4");
        root.put("codigoGeneracion", purchase.codigoGeneracion);
        root.put("fechaEmision", purchase.fecha.toString());
        root.put("tipoDocumento", "credito_fiscal_compra");

        ObjectNode emisor = root.putObject("emisor");
        emisor.put("nombre", purchase.supplierNombre);
        emisor.put("nit", purchase.supplierNit);
        emisor.put("nrc", purchase.supplierNrc);
        emisor.put("direccion", purchase.supplierDireccion);

        ObjectNode receptor = root.putObject("receptor");
        receptor.put("nombre", companyNombre);
        receptor.put("nit", companyNit);

        ArrayNode items = root.putArray("items");
        for (var item : purchase.items) {
            ObjectNode node = items.addObject();
            node.put("descripcion", item.productoNombre);
            node.put("cantidad", item.cantidad);
            node.put("precioUnitario", item.precioUnitario);
            node.put("subtotal", item.subtotal);
        }
        root.put("subtotal", purchase.subtotal);
        root.put("iva", purchase.iva);
        root.put("total", purchase.total);
        root.put("correoDestino", purchase.customerEmail);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar el crédito fiscal", ex);
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

    private String truncate(String message) {
        if (message == null || message.isBlank()) return "Error inesperado";
        return message.length() > 480 ? message.substring(0, 480) : message;
    }

    private static PurchaseDto toDto(Purchase purchase) {
        List<PurchaseItemDto> items = purchase.items.stream()
                .map(item -> new PurchaseItemDto(item.productoId, item.productoNombre, item.cantidad,
                        item.precioUnitario, item.subtotal))
                .toList();
        return new PurchaseDto(purchase.id, purchase.numeroCompra, purchase.fecha,
                purchase.supplierId, purchase.supplierNombre, purchase.customerEmail,
                purchase.emailEstado, purchase.emailFecha, purchase.emailError,
                purchase.pdf != null && !purchase.pdf.isBlank(), purchase.codigoGeneracion,
                items, nz(purchase.subtotal), nz(purchase.iva), nz(purchase.total));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
