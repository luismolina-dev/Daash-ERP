package com.daash.erp.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** Cliente simulado de Hacienda: el flag global permite probar contingencias. */
@Component
public class MockHaciendaClient {
    private final AtomicBoolean down = new AtomicBoolean(false);

    public boolean isDown() {
        return down.get();
    }

    public void setDown(boolean down) {
        this.down.set(down);
    }

    public boolean submit(String dteJson) {
        return !down.get();
    }
}
