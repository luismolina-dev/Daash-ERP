package com.daash.erp.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DteRetryJob {
    private final DteService dteService;

    public DteRetryJob(DteService dteService) {
        this.dteService = dteService;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    public void retry() {
        dteService.retryPending(false);
    }
}
