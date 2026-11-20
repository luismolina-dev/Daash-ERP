package com.daash.erp.web;

import com.daash.erp.dto.InventoryDtos.KardexDto;
import com.daash.erp.dto.InventoryDtos.InventoryAdjustmentRequest;
import com.daash.erp.dto.ProductDtos.ProductCreateRequest;
import com.daash.erp.dto.ProductDtos.ProductDto;
import com.daash.erp.dto.ProductDtos.ProductPatchRequest;
import com.daash.erp.service.KardexQueryService;
import com.daash.erp.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;
    private final KardexQueryService kardexQueryService;

    public ProductController(ProductService productService, KardexQueryService kardexQueryService) {
        this.productService = productService;
        this.kardexQueryService = kardexQueryService;
    }

    @GetMapping("/products")
    public List<ProductDto> list(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) Boolean lowStockOnly) {
        return productService.list(search, category, lowStockOnly);
    }

    @PostMapping("/products")
    public ProductDto create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/products/{id}")
    public ProductDto patch(@PathVariable Long id, @Valid @RequestBody ProductPatchRequest request) {
        return productService.patch(id, request);
    }

    @GetMapping("/inventory/kardex")
    public List<KardexDto> kardex(@RequestParam(required = false) Long productId) {
        return kardexQueryService.list(productId);
    }

    @PostMapping("/inventory/adjustments")
    public KardexDto adjust(@Valid @RequestBody InventoryAdjustmentRequest request) {
        return productService.adjust(request.productId(), request);
    }
}
