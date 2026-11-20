package com.daash.erp.service;

import com.daash.erp.model.AuditLog;
import com.daash.erp.repository.AuditLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String accion, String entidad, String entidadId, Long userId, String detalles, String ip) {
        AuditLog entry = new AuditLog();
        entry.fecha = Instant.now();
        entry.accion = accion;
        entry.entidad = entidad;
        entry.entidadId = entidadId;
        entry.userId = userId;
        entry.detalles = detalles;
        entry.ip = ip;
        auditLogRepository.save(entry);
    }

    public void log(String accion, String entidad, String entidadId, Long userId, String detalles) {
        log(accion, entidad, entidadId, userId, detalles, null);
    }
}
