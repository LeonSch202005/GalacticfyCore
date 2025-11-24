package de.galacticfy.core.service;

import org.slf4j.Logger;

public class MaintenanceService {

    private final Logger logger;
    private volatile boolean maintenanceEnabled = false;
    private String message = "§cDas Netzwerk befindet sich derzeit in Wartungsarbeiten.";

    public MaintenanceService(Logger logger) {
        this.logger = logger;
    }

    public boolean isMaintenanceEnabled() {
        return maintenanceEnabled;
    }

    public void setMaintenanceEnabled(boolean enabled) {
        this.maintenanceEnabled = enabled;
        logger.info("Maintenance-Mode wurde {}.", enabled ? "aktiviert" : "deaktiviert");
    }

    public void toggle() {
        setMaintenanceEnabled(!maintenanceEnabled);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (message != null && !message.isEmpty()) {
            this.message = message;
        }
    }
}
