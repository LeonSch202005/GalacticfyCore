package de.galacticfy.core.service;

import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Verwaltet den globalen Maintenance-Status, Timer,
 * Whitelist und pro-Server-Wartung.
 */
public class MaintenanceService {

    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Globaler Wartungsmodus
    private volatile boolean maintenanceEnabled = false;
    private volatile Long maintenanceEndMillis = null;

    // Whitelist für Spieler (Namen in lowercase)
    private final Set<String> whitelistedPlayers = ConcurrentHashMap.newKeySet();
    // Whitelist für Gruppen (Namen in lowercase)
    private final Set<String> whitelistedGroups = ConcurrentHashMap.newKeySet();

    // Pro-Server-Maintenance: Backend-Name in lowercase
    private final Set<String> serverMaintenance = ConcurrentHashMap.newKeySet();

    public MaintenanceService(Logger logger) {
        this.logger = logger;
    }

    // =====================================================================
    // GLOBALER MAINTENANCE-STATUS
    // =====================================================================

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

    // =====================================================================
    // TIMER / AUTOMATISCHES ENDE
    // =====================================================================

    /**
     * Aktiviert Maintenance jetzt und deaktiviert sie nach durationMs.
     * onStart wird beim Aktivieren (sofort) ausgeführt.
     * onEnd wird beim automatischen Ende (Timer) ausgeführt.
     */
    public synchronized void enableForDuration(long durationMs, Runnable onStart, Runnable onEnd) {
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
                logger.info("Maintenance-Zeitraum abgelaufen, deaktiviere Maintenance...");
                // Nur Auto-Ende, wenn noch aktiv (nicht manuell vorher beendet)
                if (isMaintenanceEnabled()) {
                    if (onEnd != null) {
                        try {
                            onEnd.run();
                        } catch (Exception e) {
                            logger.warn("Fehler im Maintenance-End-Callback", e);
                        }
                    }
                    setMaintenanceEnabled(false);
                }
            }, durationMs, TimeUnit.MILLISECONDS);
        } else {
            this.maintenanceEndMillis = null;
        }
    }

    /**
     * Kompatibilitäts-Methode ohne End-Callback.
     */
    public synchronized void enableForDuration(long durationMs, Runnable onStart) {
        enableForDuration(durationMs, onStart, null);
    }

    /**
     * Plant eine Maintenance:
     *  - in delayMs wird Maintenance aktiviert
     *  - läuft dann durationMs lang
     */
    public void scheduleMaintenance(long delayMs, long durationMs, Runnable onStart, Runnable onEnd) {
        logger.info("Maintenance wird in {} ms gestartet (Dauer: {} ms).", delayMs, durationMs);
        scheduler.schedule(() -> enableForDuration(durationMs, onStart, onEnd), delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Kompatibilitäts-Methode ohne End-Callback.
     */
    public void scheduleMaintenance(long delayMs, long durationMs, Runnable onStart) {
        scheduleMaintenance(delayMs, durationMs, onStart, null);
    }

    public Long getRemainingMillis() {
        if (!maintenanceEnabled || maintenanceEndMillis == null) {
            return null;
        }
        long remaining = maintenanceEndMillis - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    // =====================================================================
    // RESTZEIT FÜR MOTD – FORMATIERTE VERSION
    // =====================================================================

    public String getRemainingTimeFormatted() {
        Long remaining = getRemainingMillis();
        if (remaining == null || remaining <= 0L) {
            return "0s";
        }
        return formatDuration(remaining);
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;

        long days    = totalSeconds / 86400;
        long hours   = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    // =====================================================================
    // WHITELIST SPIELER / GRUPPEN
    // =====================================================================

    public boolean addWhitelistedPlayer(String name) {
        if (name == null || name.isBlank()) return false;
        return whitelistedPlayers.add(name.toLowerCase(Locale.ROOT));
    }

    public boolean removeWhitelistedPlayer(String name) {
        if (name == null || name.isBlank()) return false;
        return whitelistedPlayers.remove(name.toLowerCase(Locale.ROOT));
    }

    public boolean isPlayerWhitelisted(String name) {
        if (name == null) return false;
        return whitelistedPlayers.contains(name.toLowerCase(Locale.ROOT));
    }

    public List<String> getWhitelistedPlayers() {
        return whitelistedPlayers.stream()
                .sorted()
                .toList();
    }

    public boolean addWhitelistedGroup(String group) {
        if (group == null || group.isBlank()) return false;
        return whitelistedGroups.add(group.toLowerCase(Locale.ROOT));
    }

    public boolean removeWhitelistedGroup(String group) {
        if (group == null || group.isBlank()) return false;
        return whitelistedGroups.remove(group.toLowerCase(Locale.ROOT));
    }

    public Set<String> getWhitelistedGroups() {
        return Set.copyOf(whitelistedGroups);
    }

    // =====================================================================
    // PRO-SERVER-MAINTENANCE
    // =====================================================================

    public void setServerMaintenance(String backend, boolean enabled) {
        if (backend == null) return;
        backend = backend.toLowerCase(Locale.ROOT);

        if (enabled) {
            serverMaintenance.add(backend);
            logger.info("Server-Maintenance für '{}' aktiviert.", backend);
        } else {
            serverMaintenance.remove(backend);
            logger.info("Server-Maintenance für '{}' deaktiviert.", backend);
        }
    }

    public boolean isServerInMaintenance(String backend) {
        if (backend == null) return false;
        return serverMaintenance.contains(backend.toLowerCase(Locale.ROOT));
    }

    public Set<String> getServersInMaintenance() {
        return Set.copyOf(serverMaintenance);
    }
}
