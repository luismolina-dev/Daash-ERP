package com.daash.erp.web;

import com.daash.erp.dto.CashRegisterDtos.CashCloseRequest;
import com.daash.erp.dto.CashRegisterDtos.CashOpenRequest;
import com.daash.erp.dto.CashRegisterDtos.CashRegisterDto;
import com.daash.erp.service.CashRegisterService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cash-registers")
public class CashRegisterController {
    private final CashRegisterService cashRegisterService;

    public CashRegisterController(CashRegisterService cashRegisterService) {
        this.cashRegisterService = cashRegisterService;
    }

    @GetMapping("/current")
    public CashRegisterDto current() {
        return cashRegisterService.current();
    }

    @PostMapping("/open")
    public CashRegisterDto open(@Valid @RequestBody CashOpenRequest request) {
        return cashRegisterService.open(request);
    }

    @PostMapping("/close")
    public CashRegisterDto close(@Valid @RequestBody CashCloseRequest request) {
        return cashRegisterService.close(request);
    }
}
