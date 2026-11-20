package com.daash.erp.service;

import com.daash.erp.dto.CustomerDtos.CustomerCreateRequest;
import com.daash.erp.dto.CustomerDtos.CustomerDto;
import com.daash.erp.model.Customer;
import com.daash.erp.repository.CustomerRepository;
import com.daash.erp.repository.UserRepository;
import com.daash.erp.web.AuthContext;
import com.daash.erp.web.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository,
                           AuditService auditService) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> list() {
        return customerRepository.findAll().stream().map(CustomerDto::from).toList();
    }

    @Transactional
    public CustomerDto create(CustomerCreateRequest request) {
        boolean hasDui = request.dui() != null && !request.dui().isBlank();
        boolean hasNit = request.nit() != null && !request.nit().isBlank();
        if (!hasDui && !hasNit) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Debe proporcionar DUI o NIT");
        }
        Customer customer = new Customer();
        customer.nombre = request.nombre().trim();
        customer.email = request.email();
        customer.telefono = request.telefono();
        customer.dui = hasDui ? request.dui().trim() : null;
        customer.nit = hasNit ? request.nit().trim() : null;
        customer.nrc = request.nrc();
        customer.direccion = request.direccion();
        customer.tipoContribuyente = request.tipoContribuyente() == null || request.tipoContribuyente().isBlank()
                ? "consumidor_final" : request.tipoContribuyente().trim();
        Customer saved = customerRepository.save(customer);
        String email = AuthContext.currentEmail();
        Long userId = email == null ? null : userRepository.findByEmail(email).map(user -> user.id).orElse(null);
        auditService.log("CREAR_CLIENTE", "Customer", saved.id.toString(), userId, "Cliente creado: " + saved.nombre);
        return CustomerDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }
}
