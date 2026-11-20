package com.daash.erp.web;

import com.daash.erp.dto.SaleDtos.SaleCreateRequest;
import com.daash.erp.dto.SaleDtos.SaleDto;
import com.daash.erp.service.SaleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
public class SaleController {
    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public SaleDto create(@Valid @RequestBody SaleCreateRequest request) {
        return saleService.create(request);
    }

    @GetMapping
    public List<SaleDto> list(@RequestParam(required = false) String search,
                              @RequestParam(required = false) String estado) {
        return saleService.list(search, estado);
    }

    @PostMapping("/{id}/anular")
    public SaleDto anular(@PathVariable Long id) {
        return saleService.anular(id);
    }
}
