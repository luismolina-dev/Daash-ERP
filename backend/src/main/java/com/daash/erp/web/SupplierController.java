package com.daash.erp.web;

import com.daash.erp.dto.SupplierDtos.SupplierCreateRequest;
import com.daash.erp.dto.SupplierDtos.SupplierDto;
import com.daash.erp.service.SupplierService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierDto> list() {
        return supplierService.list();
    }

    @PostMapping
    public SupplierDto create(@Valid @RequestBody SupplierCreateRequest request) {
        return supplierService.create(request);
    }
}
