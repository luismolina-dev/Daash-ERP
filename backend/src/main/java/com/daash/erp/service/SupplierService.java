package com.daash.erp.service;

import com.daash.erp.dto.SupplierDtos.SupplierCreateRequest;
import com.daash.erp.dto.SupplierDtos.SupplierDto;
import com.daash.erp.model.Supplier;
import com.daash.erp.repository.SupplierRepository;
import com.daash.erp.repository.UserRepository;
import com.daash.erp.web.AuthContext;
import com.daash.erp.web.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public SupplierService(SupplierRepository supplierRepository, UserRepository userRepository,
                           AuditService auditService) {
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> list() {
        return supplierRepository.findAll().stream().map(SupplierDto::from).toList();
    }

    @Transactional
    public SupplierDto create(SupplierCreateRequest request) {
        Supplier supplier = new Supplier();
        supplier.nombre = request.nombre().trim();
        supplier.nit = request.nit();
        supplier.nrc = request.nrc();
        supplier.telefono = request.telefono();
        supplier.email = request.email();
        supplier.direccion = request.direccion();
        supplier.contacto = request.contacto();
        Supplier saved = supplierRepository.save(supplier);
        String email = AuthContext.currentEmail();
        Long userId = email == null ? null : userRepository.findByEmail(email).map(user -> user.id).orElse(null);
        auditService.log("CREAR_PROVEEDOR", "Supplier", saved.id.toString(), userId, "Proveedor creado: " + saved.nombre);
        return SupplierDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
    }
}
