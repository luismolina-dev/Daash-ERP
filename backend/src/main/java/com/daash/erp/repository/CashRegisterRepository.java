package com.daash.erp.repository;

import com.daash.erp.model.CashRegister;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {
    Optional<CashRegister> findByAbiertaTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashRegister c where c.abierta = true")
    Optional<CashRegister> findByAbiertaTrueForUpdate();
}
