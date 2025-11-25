package de.galacticfy.core.util;

import org.slf4j.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Discord-Webhook-Sender mit Embeds für Maintenance:
 * - geplant
 * - gestartet
 * - beendet
 */
public class DiscordWebhookNotifier {

    private final String webhookUrl;
    private final Logger logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DiscordWebhookNotifier(Logger logger, String webhookUrl) {
        this.logger = logger;
        this.webhookUrl = webhookUrl;
    }

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    /**
     * Geplante Wartung: Wartung wird in X starten.
     *
     * @param by       Wer hat geplant
     * @param duration Dauer-Text (z.B. "30m" oder "1h 30m")
     * @param startsIn Start-Text (z.B. "10m")
     */
    public void sendMaintenancePlanned(String by, String duration, String startsIn) {
        if (!isEnabled()) {
            return;
        }

        String safeBy = (by == null || by.isBlank()) ? "Unbekannt" : by;
        String safeDuration = (duration == null || duration.isBlank()) ? "—" : duration;
        String safeStartsIn = (startsIn == null || startsIn.isBlank()) ? "bald" : startsIn;

        String title = "🟧 Maintenance geplant";
        String description = "Eine geplante Wartung wurde eingerichtet.\n"
                + "Das **Galacticfy** Netzwerk wird in Kürze gewartet.";

        String footerText = "GalacticfyCore • Maintenance";
        String timestamp = Instant.now().toString();
        int color = 0xFAA61A; // Orange

        String json = buildPlannedEmbedPayload(
                title,
                description,
                safeBy,
                safeDuration,
                safeStartsIn,
                footerText,
                timestamp,
                color
        );

        sendAsync(json);
    }

    /**
     * Wartung ist jetzt aktiv.
     *
     * @param by       Wer hat gestartet
     * @param duration Dauer-Text (z.B. "30m" oder "unbekannt")
     */
    public void sendMaintenanceStarted(String by, String duration) {
        if (!isEnabled()) {
            return;
        }

        String safeBy = (by == null || by.isBlank()) ? "Unbekannt" : by;
        String safeDuration = (duration == null || duration.isBlank()) ? "—" : duration;

        String title = "🟩 Maintenance jetzt aktiv";
        String description = "Die Wartung hat begonnen.\n"
                + "Das **Galacticfy** Netzwerk befindet sich nun im Wartungsmodus.";

        String footerText = "GalacticfyCore • Maintenance";
        String timestamp = Instant.now().toString();
        int color = 0x57F287; // Grün

        String json = buildStartedEmbedPayload(
                title,
                description,
                safeBy,
                safeDuration,
                footerText,
                timestamp,
                color
        );

        sendAsync(json);
    }

    /**
     * Wartung beendet.
     *
     * @param by Wer hat beendet
     */
    public void sendMaintenanceEnd(String by) {
        if (!isEnabled()) {
            return;
        }

        String safeBy = (by == null || by.isBlank()) ? "Unbekannt" : by;

        String title = "✅ Maintenance beendet";
        String description = "Die Wartung ist abgeschlossen.\n"
                + "Das **Galacticfy** Netzwerk ist wieder verfügbar.";

        String footerText = "GalacticfyCore • Maintenance";
        String timestamp = Instant.now().toString();
        int color = 0x57F287; // Grün

        String json = buildEndEmbedPayload(
                title,
                description,
                safeBy,
                footerText,
                timestamp,
                color
        );

        sendAsync(json);
    }

    // ------------------------------------------------------------------------
    // Embed-Payloads
    // ------------------------------------------------------------------------

    private String buildPlannedEmbedPayload(
            String title,
            String description,
            String by,
            String duration,
            String startsIn,
            String footerText,
            String timestamp,
            int color
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        sb.append("\"color\":").append(color).append(",");

        // Felder
        sb.append("\"fields\":[");
        // Ausgeführt von
        sb.append("{")
                .append("\"name\":\"").append(escapeJson("👤 Ausgeführt von")).append("\",")
                .append("\"value\":\"").append(escapeJson("• `" + by + "`")).append("\",")
                .append("\"inline\":false")
                .append("},");

        // Dauer
        sb.append("{")
                .append("\"name\":\"").append(escapeJson("⏱ Dauer")).append("\",")
                .append("\"value\":\"").append(escapeJson("• " + duration)).append("\",")
                .append("\"inline\":true")
                .append("},");

        // Beginn in
        sb.append("{")
                .append("\"name\":\"").append(escapeJson("⏰ Beginn in")).append("\",")
                .append("\"value\":\"").append(escapeJson("• " + startsIn)).append("\",")
                .append("\"inline\":true")
                .append("}");
        sb.append("],");

        // Footer
        sb.append("\"footer\":{")
                .append("\"text\":\"").append(escapeJson(footerText)).append("\"")
                .append("},");

        // Timestamp
        sb.append("\"timestamp\":\"").append(escapeJson(timestamp)).append("\"");

        sb.append("}]"); // embeds
        sb.append("}");

        return sb.toString();
    }

    private String buildStartedEmbedPayload(
            String title,
            String description,
            String by,
            String duration,
            String footerText,
            String timestamp,
            int color
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        sb.append("\"color\":").append(color).append(",");

        // Felder
        sb.append("\"fields\":[");
        // Ausgeführt von
        sb.append("{")
                .append("\"name\":\"").append(escapeJson("👤 Ausgeführt von")).append("\",")
                .append("\"value\":\"").append(escapeJson("• `" + by + "`")).append("\",")
                .append("\"inline\":false")
                .append("},");

        // Dauer
        sb.append("{")
                .append("\"name\":\"").append(escapeJson("⏱ Dauer")).append("\",")
                .append("\"value\":\"").append(escapeJson("• " + duration)).append("\",")
                .append("\"inline\":true")
                .append("}");
        sb.append("],");

        // Footer + Zeit
        sb.append("\"footer\":{")
                .append("\"text\":\"").append(escapeJson(footerText)).append("\"")
                .append("},");
        sb.append("\"timestamp\":\"").append(escapeJson(timestamp)).append("\"");

        sb.append("}]");
        sb.append("}");

        return sb.toString();
    }

    private String buildEndEmbedPayload(
            String title,
            String description,
            String by,
            String footerText,
            String timestamp,
            int color
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        sb.append("\"color\":").append(color).append(",");

        sb.append("\"fields\":[");
        sb.append("{")
                .append("\"name\":\"").append(escapeJson("👤 Beendet von")).append("\",")
                .append("\"value\":\"").append(escapeJson("• `" + by + "`")).append("\",")
                .append("\"inline\":false")
                .append("}");
        sb.append("],");

        sb.append("\"footer\":{")
                .append("\"text\":\"").append(escapeJson(footerText)).append("\"")
                .append("},");
        sb.append("\"timestamp\":\"").append(escapeJson(timestamp)).append("\"");

        sb.append("}]");
        sb.append("}");

        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // HTTP
    // ------------------------------------------------------------------------

    private void sendAsync(String json) {
        executor.submit(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                byte[] out = json.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(out.length);
                conn.connect();

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(out);
                }

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    logger.warn("Discord Webhook antwortete mit HTTP-Code {}", code);
                }
            } catch (Exception e) {
                logger.warn("Konnte Discord-Webhook nicht senden", e);
            }
        });
    }

    private String escapeJson(String s) {
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
