package com.daash.erp.service;

import com.daash.erp.dto.SaleDtos.SaleCreateRequest;
import com.daash.erp.dto.SaleDtos.SaleDto;
import com.daash.erp.dto.SaleDtos.SaleItemRequest;
import com.daash.erp.model.Customer;
import com.daash.erp.model.Product;
import com.daash.erp.model.Role;
import com.daash.erp.model.Sale;
import com.daash.erp.model.SaleItem;
import com.daash.erp.model.User;
import com.daash.erp.repository.CustomerRepository;
import com.daash.erp.repository.ProductRepository;
import com.daash.erp.repository.SaleRepository;
import com.daash.erp.repository.UserRepository;
import com.daash.erp.web.AuthContext;
import com.daash.erp.web.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {
    private static final BigDecimal IVA_RATE = new BigDecimal("0.13");
    private static final BigDecimal DISCOUNT_LIMIT = new BigDecimal("10");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CashRegisterService cashRegisterService;
    private final InventoryService inventoryService;
    private final DteService dteService;
    private final AuditService auditService;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository,
                       CustomerRepository customerRepository, UserRepository userRepository,
                       CashRegisterService cashRegisterService, InventoryService inventoryService,
                       DteService dteService, AuditService auditService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.cashRegisterService = cashRegisterService;
        this.inventoryService = inventoryService;
        this.dteService = dteService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SaleDto> list(String search, String estado) {
        String query = search == null ? "" : search.toLowerCase(Locale.ROOT);
        String state = estado == null ? "" : estado.trim().toLowerCase(Locale.ROOT);
        return saleRepository.findAllByOrderByFechaDesc().stream()
                .filter(sale -> state.isBlank() || state.equalsIgnoreCase(sale.estadoDTE))
                .filter(sale -> query.isBlank()
                        || sale.numeroFactura.toLowerCase(Locale.ROOT).contains(query)
                        || (sale.clienteNombre != null && sale.clienteNombre.toLowerCase(Locale.ROOT).contains(query)))
                .map(SaleDto::from)
                .toList();
    }

    /** CA-05: cualquier precio enviado por el cliente se ignora; se recalcula desde products.precio. */
    @Transactional
    public SaleDto create(SaleCreateRequest request) {
        cashRegisterService.findOpenOrThrow();
        User user = currentUser();
        boolean manager = user != null && user.role == Role.GERENTE;

        List<ResolvedItem> resolved = new ArrayList<>();
        for (SaleItemRequest item : request.items()) {
            Product product = resolveProduct(item.productId());
            if (!product.activo) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "El producto " + product.nombre + " no está activo");
            }
            BigDecimal descuento = item.descuento() == null ? BigDecimal.ZERO : item.descuento();
            if (descuento.compareTo(BigDecimal.ZERO) < 0 || descuento.compareTo(HUNDRED) > 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "El descuento debe estar entre 0 y 100");
            }
            if (descuento.compareTo(DISCOUNT_LIMIT) > 0 && !manager) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Un descuento mayor al 10% solo puede ser autorizado por el gerente");
            }
            if (product.stock < item.cantidad()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para " + product.nombre + " (disponible: " + product.stock + ")");
            }
            BigDecimal base = product.precio.multiply(BigDecimal.valueOf(item.cantidad()));
            BigDecimal monto = base.multiply(BigDecimal.ONE.subtract(descuento.divide(HUNDRED)))
                    .setScale(2, RoundingMode.HALF_UP);
            resolved.add(new ResolvedItem(product, item.cantidad(), descuento, monto));
        }

        BigDecimal subtotal = resolved.stream().map(ResolvedItem::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = subtotal.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva);

        Customer customer = resolveCustomer(request.customerId());
        Sale sale = new Sale();
        sale.numeroFactura = nextInvoiceNumber();
        sale.fecha = Instant.now();
        sale.clienteId = customer == null ? null : customer.id;
        sale.clienteNombre = customer == null ? "Consumidor Final" : customer.nombre;
        sale.metodoPago = request.metodoPago();
        sale.subtotal = subtotal;
        sale.iva = iva;
        sale.total = total;
        sale.userId = user == null ? null : user.id;
        sale.estadoDTE = "pendiente";
        sale.anulada = false;

        for (ResolvedItem item : resolved) {
            SaleItem saleItem = new SaleItem();
            saleItem.sale = sale;
            saleItem.productoId = item.product().id;
            saleItem.productoCodigo = item.product().codigo;
            saleItem.productoNombre = item.product().nombre;
            saleItem.cantidad = item.cantidad();
            saleItem.precioUnitario = item.product().precio;
            saleItem.descuento = item.descuento();
            saleItem.montoLinea = item.monto();
            sale.items.add(saleItem);
        }

        Sale saved = saleRepository.saveAndFlush(sale);
        for (ResolvedItem item : resolved) {
            inventoryService.move(item.product(), -item.cantidad(), "venta",
                    "Venta " + saved.numeroFactura, saved.userId, saved.numeroFactura);
        }
        cashRegisterService.registerSale(total, request.metodoPago());
        dteService.issueDte(saved);
        audit("VENTA", saved, "Factura " + saved.numeroFactura + " por " + total);
        return SaleDto.from(saved);
    }

    @Transactional
    public SaleDto anular(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        if (sale.anulada) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La venta ya está anulada");
        }
        sale.notaCreditoId = "NC-" + sale.numeroFactura;
        sale.anulada = true;
        for (SaleItem item : sale.items) {
            Product product = productRepository.findById(item.productoId).orElse(null);
            if (product != null) {
                inventoryService.move(product, item.cantidad, "devolucion",
                        "Anulación de factura " + sale.numeroFactura + " - Nota de Crédito",
                        sale.userId, sale.notaCreditoId);
            }
        }
        Sale saved = saleRepository.save(sale);
        audit("ANULACION_VENTA", saved, "Nota de crédito " + saved.notaCreditoId);
        return SaleDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Sale findById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
    }

    private Product resolveProduct(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "productId es obligatorio");
        }
        try {
            Long id = Long.valueOf(rawId.trim());
            return productRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        } catch (NumberFormatException ex) {
            return productRepository.findByCodigo(rawId.trim())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        }
    }

    private Customer resolveCustomer(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        try {
            Long id = Long.valueOf(rawId.trim());
            return customerRepository.findById(id).orElse(null);
        } catch (NumberFormatException ex) {
            return customerRepository.findByEmail(rawId.trim())
                    .or(() -> customerRepository.findByDui(rawId.trim()))
                    .or(() -> customerRepository.findByNit(rawId.trim()))
                    .orElse(null);
        }
    }

    private String nextInvoiceNumber() {
        long sequence = saleRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format(Locale.ROOT, "FE001-%04d", sequence++);
        } while (saleRepository.existsByNumeroFactura(candidate));
        return candidate;
    }

    private User currentUser() {
        String email = AuthContext.currentEmail();
        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    private void audit(String action, Sale sale, String details) {
        auditService.log(action, "Sale", sale.id == null ? null : sale.id.toString(), sale.userId, details);
    }

    private record ResolvedItem(Product product, Integer cantidad, BigDecimal descuento, BigDecimal monto) { }
}
