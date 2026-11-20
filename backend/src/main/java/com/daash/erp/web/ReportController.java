package com.daash.erp.web;

import com.daash.erp.dto.ReportDtos.SummaryDto;
import com.daash.erp.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public SummaryDto summary() {
        return reportService.summary();
    }

    @GetMapping(value = "/iva", produces = "text/csv")
    public String iva(@RequestParam String tipo) {
        return reportService.ivaCsv(tipo);
    }
}
