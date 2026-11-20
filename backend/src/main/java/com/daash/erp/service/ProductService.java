package com.daash.erp.service;

import com.daash.erp.dto.InventoryDtos.KardexDto;
import com.daash.erp.dto.ProductDtos.ProductCreateRequest;
import com.daash.erp.dto.ProductDtos.ProductDto;
import com.daash.erp.dto.ProductDtos.ProductPatchRequest;
import com.daash.erp.model.Product;
import com.daash.erp.model.User;
import com.daash.erp.repository.ProductRepository;
import com.daash.erp.repository.UserRepository;
import com.daash.erp.web.AuthContext;
import com.daash.erp.web.BusinessException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository, UserRepository userRepository,
                          InventoryService inventoryService, AuditService auditService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> list(String search, String category, Boolean lowStockOnly) {
        String query = search == null ? "" : search.toLowerCase(Locale.ROOT);
        String cat = category == null ? "" : category.toLowerCase(Locale.ROOT);
        return productRepository.findAll().stream()
                .filter(product -> query.isBlank()
                        || product.nombre.toLowerCase(Locale.ROOT).contains(query)
                        || (product.codigo != null && product.codigo.toLowerCase(Locale.ROOT).contains(query)))
                .filter(product -> cat.isBlank()
                        || (product.categoria != null && product.categoria.toLowerCase(Locale.ROOT).equals(cat)))
                .filter(product -> !Boolean.TRUE.equals(lowStockOnly)
                        || product.stock <= product.stockMinimo)
                .sorted(Comparator.comparing(product -> product.nombre, String.CASE_INSENSITIVE_ORDER))
                .map(ProductDto::from)
                .toList();
    }

    @Transactional
    public ProductDto create(ProductCreateRequest request) {
        if (productRepository.existsByCodigo(request.codigo())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un producto con ese código");
        }
        Product product = new Product();
        product.codigo = request.codigo().trim();
        product.nombre = request.nombre().trim();
        product.descripcion = request.descripcion();
        product.categoria = request.categoria().trim();
        product.precio = request.precio();
        product.costo = request.costo();
        product.stock = 0;
        product.stockMinimo = request.stockMinimo();
        product.activo = request.activo() == null || request.activo();
        Product saved = productRepository.save(product);
        audit("CREAR_PRODUCTO", saved.id, "Producto creado: " + saved.codigo);
        return ProductDto.from(saved);
    }

    @Transactional
    public ProductDto patch(Long id, ProductPatchRequest request) {
        Product product = findById(id);
        if (request.nombre() != null) product.nombre = request.nombre();
        if (request.descripcion() != null) product.descripcion = request.descripcion();
        if (request.categoria() != null) product.categoria = request.categoria();
        if (request.precio() != null) product.precio = request.precio();
        if (request.costo() != null) product.costo = request.costo();
        if (request.stockMinimo() != null) product.stockMinimo = request.stockMinimo();
        if (request.activo() != null) product.activo = request.activo();
        // stock NO se modifica aquí: use /inventory/adjustments.
        Product saved = productRepository.save(product);
        audit("EDITAR_PRODUCTO", saved.id, "Producto actualizado: " + saved.codigo);
        return ProductDto.from(saved);
    }

    @Transactional
    public KardexDto adjust(Long id, com.daash.erp.dto.InventoryDtos.InventoryAdjustmentRequest request) {
        Product product = findById(id);
        if (request.tipo() == null || request.tipo().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "tipo es obligatorio");
        }
        String tipo = request.tipo().trim().toLowerCase(Locale.ROOT);
        int delta = switch (tipo) {
            case "salida", "salida_inventario", "descarga" -> -request.cantidad();
            case "entrada", "entrada_inventario", "carga", "ajuste" -> request.cantidad();
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Tipo de ajuste no soportado: " + tipo);
        };
        KardexDto result = inventoryService.move(product, delta, tipo,
                request.justificacion(), currentUserId(),
                "AJUSTE-" + id + "-" + System.currentTimeMillis());
        audit("AJUSTE_INVENTARIO", id,
                "Ajuste " + tipo + " de " + request.cantidad() + " - " + request.justificacion());
        return result;
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private Long currentUserId() {
        String email = AuthContext.currentEmail();
        if (email == null) return null;
        return userRepository.findByEmail(email).map(user -> user.id).orElse(null);
    }

    private void audit(String action, Long entityId, String details) {
        auditService.log(action, "Product", entityId == null ? null : entityId.toString(), currentUserId(), details);
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
