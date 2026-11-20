package com.daash.erp.web;

import com.daash.erp.dto.CustomerDtos.CustomerCreateRequest;
import com.daash.erp.dto.CustomerDtos.CustomerDto;
import com.daash.erp.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerDto> list() {
        return customerService.list();
    }

    @PostMapping
    public CustomerDto create(@Valid @RequestBody CustomerCreateRequest request) {
        return customerService.create(request);
    }
}
