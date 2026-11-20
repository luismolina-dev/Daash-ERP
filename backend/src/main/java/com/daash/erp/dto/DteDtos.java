package com.daash.erp.dto;

import com.daash.erp.model.ContingencyQueue;
import java.time.Instant;

public final class DteDtos {
    private DteDtos() { }

    public record ContingencyDto(Long id, Long saleId, Integer intentos, Integer maxIntentos, String estado,
                                 Instant proximoReintento, String ultimoError) {
        public static ContingencyDto from(ContingencyQueue queue) {
            return new ContingencyDto(queue.id, queue.saleId, queue.intentos, queue.maxIntentos, queue.estado,
                    queue.proximoReintento, queue.ultimoError);
        }
    }

    public record DowntimeRequest(boolean down) { }

    public record DowntimeResponse(boolean down) { }
}
