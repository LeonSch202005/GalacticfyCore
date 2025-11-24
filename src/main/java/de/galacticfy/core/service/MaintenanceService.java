package de.galacticfy.core.service;

import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Verwaltet den globalen Maintenance-Status inkl. Timer.
 */
public class MaintenanceService {

    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean maintenanceEnabled = false;
    private volatile Long maintenanceEndMillis = null;

    public MaintenanceService(Logger logger) {
        this.logger = logger;
    }

    public boolean isMaintenanceEnabled() {
        return maintenanceEnabled;
    }

    /**
     * Schaltet Maintenance sofort an/aus, ohne Timer.
     */
    public synchronized void setMaintenanceEnabled(boolean enabled) {
        this.maintenanceEnabled = enabled;
        if (!enabled) {
            this.maintenanceEndMillis = null;
        }
        logger.info("Maintenance-Mode wurde {}.", enabled ? "aktiviert" : "deaktiviert");
    }

    /**
     * Aktiviert Maintenance jetzt und deaktiviert sie nach durationMs.
     * onStart wird beim Aktivieren (sofort) ausgeführt.
     */
    public synchronized void enableForDuration(long durationMs, Runnable onStart) {
        setMaintenanceEnabled(true);

        if (onStart != null) {
            try {
                onStart.run();
            } catch (Exception e) {
                logger.warn("Fehler im Maintenance-Start-Callback", e);
            }
        }

        if (durationMs > 0) {
            long endAt = System.currentTimeMillis() + durationMs;
            this.maintenanceEndMillis = endAt;

            scheduler.schedule(() -> {
                logger.info("Maintenance-Zeitraum abgelaufen, deaktivieren...");
                setMaintenanceEnabled(false);
            }, durationMs, TimeUnit.MILLISECONDS);
        } else {
            this.maintenanceEndMillis = null;
        }
    }

    /**
     * Plant eine Maintenance:
     *  - in delayMs wird Maintenance aktiviert
     *  - läuft dann durationMs lang
     *  - onStart wird beim Start ausgeführt
     */
    public void scheduleMaintenance(long delayMs, long durationMs, Runnable onStart) {
        logger.info("Maintenance wird in {} ms gestartet (Dauer: {} ms).", delayMs, durationMs);
        scheduler.schedule(() -> enableForDuration(durationMs, onStart), delayMs, TimeUnit.MILLISECONDS);
    }

    public Long getRemainingMillis() {
        if (!maintenanceEnabled || maintenanceEndMillis == null) {
            return null;
        }
        long remaining = maintenanceEndMillis - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}
