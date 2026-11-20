package com.daash.erp.web;

import com.daash.erp.dto.InventoryDtos.PurchaseDto;
import com.daash.erp.dto.InventoryDtos.PurchaseRequest;
import com.daash.erp.service.PurchaseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public PurchaseDto create(@Valid @RequestBody PurchaseRequest request) {
        return purchaseService.create(request);
    }

    @GetMapping
    public List<PurchaseDto> list() {
        return purchaseService.list();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] pdf = purchaseService.pdf(id);
        String filename = "factura-compra-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/resend-email")
    public PurchaseDto resendEmail(@PathVariable Long id) {
        return purchaseService.resendEmail(id);
    }
}
