package com.daash.erp.dto;

import com.daash.erp.model.CashRegister;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class CashRegisterDtos {
    private CashRegisterDtos() { }

    public record ArqueoDto(Instant fecha, BigDecimal efectivoEsperado, BigDecimal efectivoReal,
                            BigDecimal tarjeta, BigDecimal transferencia, BigDecimal totalVentas,
                            BigDecimal diferencia) { }

    public record CashRegisterDto(Long id, Long userId, Instant fechaApertura, Instant fechaCierre,
                                  BigDecimal fondoInicial, BigDecimal totalEfectivo, BigDecimal totalTarjeta,
                                  BigDecimal totalTransferencia, BigDecimal totalVentas, Boolean abierta,
                                  ArqueoDto arqueo) {
        public static CashRegisterDto from(CashRegister register) {
            ArqueoDto arqueo = null;
            if (register.fechaCierre != null) {
                arqueo = new ArqueoDto(register.fechaArqueo, register.efectivoEsperado, register.efectivoReal,
                        register.tarjetaReal, register.transferenciaReal, register.totalVentas, register.diferencia);
            }
            return new CashRegisterDto(register.id, register.userId, register.fechaApertura, register.fechaCierre,
                    register.fondoInicial, register.totalEfectivo, register.totalTarjeta,
                    register.totalTransferencia, register.totalVentas, register.abierta, arqueo);
        }
    }

    public record CashOpenRequest(@NotNull @DecimalMin("0") BigDecimal fondoInicial) { }

    public record CashCloseRequest(@NotNull @DecimalMin("0") BigDecimal efectivoReal,
                                   @NotNull @DecimalMin("0") BigDecimal tarjetaReal,
                                   @NotNull @DecimalMin("0") BigDecimal transferenciaReal) { }
}
