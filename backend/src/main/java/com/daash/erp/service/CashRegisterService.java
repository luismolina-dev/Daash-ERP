package com.daash.erp.service;

import com.daash.erp.dto.CashRegisterDtos.CashCloseRequest;
import com.daash.erp.dto.CashRegisterDtos.CashOpenRequest;
import com.daash.erp.dto.CashRegisterDtos.CashRegisterDto;
import com.daash.erp.model.CashRegister;
import com.daash.erp.repository.CashRegisterRepository;
import com.daash.erp.repository.UserRepository;
import com.daash.erp.web.AuthContext;
import com.daash.erp.web.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashRegisterService {
    private final CashRegisterRepository cashRegisterRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CashRegisterService(CashRegisterRepository cashRegisterRepository, UserRepository userRepository,
                               AuditService auditService) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public CashRegisterDto current() {
        return cashRegisterRepository.findByAbiertaTrue().map(CashRegisterDto::from).orElse(null);
    }

    @Transactional
    public CashRegisterDto open(CashOpenRequest request) {
        if (cashRegisterRepository.findByAbiertaTrueForUpdate().isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Ya existe una caja abierta");
        }
        CashRegister register = new CashRegister();
        register.userId = currentUserId();
        register.fechaApertura = Instant.now();
        register.fondoInicial = request.fondoInicial();
        register.abierta = true;
        CashRegister saved = cashRegisterRepository.save(register);
        audit("APERTURA_CAJA", saved.id, "Apertura con fondo " + request.fondoInicial());
        return CashRegisterDto.from(saved);
    }

    @Transactional
    public CashRegisterDto close(CashCloseRequest request) {
        CashRegister register = cashRegisterRepository.findByAbiertaTrueForUpdate()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No hay una caja abierta"));
        BigDecimal esperadoEfectivo = register.fondoInicial.add(register.totalEfectivo);
        BigDecimal esperadoTarjeta = register.totalTarjeta;
        BigDecimal esperadoTransferencia = register.totalTransferencia;
        BigDecimal esperadoTotal = esperadoEfectivo.add(esperadoTarjeta).add(esperadoTransferencia);
        BigDecimal realTotal = request.efectivoReal().add(request.tarjetaReal()).add(request.transferenciaReal());

        register.fechaCierre = Instant.now();
        register.fechaArqueo = register.fechaCierre;
        register.efectivoEsperado = esperadoEfectivo;
        register.efectivoReal = request.efectivoReal();
        register.tarjetaReal = request.tarjetaReal();
        register.transferenciaReal = request.transferenciaReal();
        register.diferencia = realTotal.subtract(esperadoTotal);
        register.abierta = false;
        CashRegister saved = cashRegisterRepository.save(register);
        audit("CIERRE_CAJA", saved.id, "Arqueo: esperado=" + esperadoTotal + ", real=" + realTotal
                + ", diferencia=" + register.diferencia);
        return CashRegisterDto.from(saved);
    }

    @Transactional(readOnly = true)
    public CashRegister findOpenOrThrow() {
        return cashRegisterRepository.findByAbiertaTrue()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "No hay una caja abierta; abra una caja antes de vender"));
    }

    @Transactional
    public void registerSale(BigDecimal total, String metodoPago) {
        CashRegister register = cashRegisterRepository.findByAbiertaTrueForUpdate()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No hay una caja abierta"));
        switch (metodoPago) {
            case "efectivo" -> register.totalEfectivo = register.totalEfectivo.add(total);
            case "tarjeta" -> register.totalTarjeta = register.totalTarjeta.add(total);
            case "transferencia" -> register.totalTransferencia = register.totalTransferencia.add(total);
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Método de pago inválido");
        }
        register.totalVentas = register.totalVentas.add(total);
        cashRegisterRepository.save(register);
    }

    private Long currentUserId() {
        String email = AuthContext.currentEmail();
        if (email == null) return null;
        return userRepository.findByEmail(email).map(user -> user.id).orElse(null);
    }

    private void audit(String action, Long entityId, String details) {
        auditService.log(action, "CashRegister", entityId == null ? null : entityId.toString(),
                currentUserId(), details);
    }
}
