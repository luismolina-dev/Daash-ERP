package com.daash.erp.web;

import com.daash.erp.dto.DteDtos.ContingencyDto;
import com.daash.erp.dto.DteDtos.DowntimeRequest;
import com.daash.erp.dto.DteDtos.DowntimeResponse;
import com.daash.erp.repository.ContingencyQueueRepository;
import com.daash.erp.service.MockHaciendaClient;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dte")
public class DteController {
    private final MockHaciendaClient haciendaClient;
    private final ContingencyQueueRepository contingencyQueueRepository;

    public DteController(MockHaciendaClient haciendaClient,
                         ContingencyQueueRepository contingencyQueueRepository) {
        this.haciendaClient = haciendaClient;
        this.contingencyQueueRepository = contingencyQueueRepository;
    }

    @GetMapping("/contingencies")
    public List<ContingencyDto> contingencies() {
        return contingencyQueueRepository.findAll().stream().map(ContingencyDto::from).toList();
    }

    @PostMapping("/simulation/downtime")
    public DowntimeResponse downtime(@Valid @RequestBody DowntimeRequest request) {
        haciendaClient.setDown(request.down());
        return new DowntimeResponse(haciendaClient.isDown());
    }
}
