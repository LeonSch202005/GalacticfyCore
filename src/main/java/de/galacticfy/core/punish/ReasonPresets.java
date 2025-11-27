package de.galacticfy.core.punish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Zentrale Reason-Presets mit:
 *  - Key (für Command: /ban <Spieler> <key>)
 *  - Anzeige-Text
 *  - Default-Dauer (ms, kann null = permanent)
 *  - Schweregrad (LIGHT / MEDIUM / HEAVY)
 *
 * Beispiele:
 *   /ban Leon chatspam
 *   /ban Leon beleidigung
 *   /ban Leon hackclient
 */
public final class ReasonPresets {

    public enum Severity {
        LIGHT,
        MEDIUM,
        HEAVY
    }

    public static final class Preset {

        private final String key;
        private final String display;
        private final Long defaultDurationMs;
        private final Severity severity;

        public Preset(String key, String display, Long defaultDurationMs, Severity severity) {
            this.key = key;
            this.display = display;
            this.defaultDurationMs = defaultDurationMs;
            this.severity = severity;
        }

        public String key() {
            return key;
        }

        public String display() {
            return display;
        }

        /**
         * Kann null sein → permanent.
         */
        public Long defaultDurationMs() {
            return defaultDurationMs;
        }

        public Severity severity() {
            return severity;
        }
    }

    // ============================================================
    // PRESET-DEFINITIONEN
    // ============================================================

    private static final List<Preset> PRESETS;

    static {
        List<Preset> list = new ArrayList<>();

        // leichte Verstöße
        list.add(new Preset(
                "chatspam",
                "Chat-Spam",
                minutes(30),
                Severity.LIGHT
        ));
        list.add(new Preset(
                "commandspam",
                "Command-Spam",
                minutes(45),
                Severity.LIGHT
        ));
        list.add(new Preset(
                "caps",
                "Übermäßige Capslock-Nutzung",
                minutes(20),
                Severity.LIGHT
        ));
        list.add(new Preset(
                "werbung",
                "Fremdwerbung / Werbung",
                minutes(90),
                Severity.MEDIUM
        ));

        // Beleidigung / Respekt
        list.add(new Preset(
                "beleidigung",
                "Beleidigung / Respektlosigkeit",
                minutes(60),
                Severity.MEDIUM
        ));
        list.add(new Preset(
                "teambeleidigung",
                "Beleidigung gegenüber Teammitgliedern",
                minutes(120),
                Severity.HEAVY
        ));
        list.add(new Preset(
                "provokation",
                "Provokation / Flame",
                minutes(45),
                Severity.MEDIUM
        ));

        // Diskriminierung / harte Themen
        list.add(new Preset(
                "rassismus",
                "Rassistische / diskriminierende Äußerungen",
                days(1),
                Severity.HEAVY
        ));
        list.add(new Preset(
                "ns",
                "Nationalsozialistische Inhalte / Symbole",
                days(3),
                Severity.HEAVY
        ));

        // Gameplay / Hacks
        list.add(new Preset(
                "bugusing",
                "Bugusing / Ausnutzen von Fehlern",
                days(3),
                Severity.HEAVY
        ));
        list.add(new Preset(
                "griefing",
                "Griefing / mutwillige Zerstörung",
                days(2),
                Severity.MEDIUM
        ));
        list.add(new Preset(
                "hackclient",
                "Unerlaubte Modifikation / Hack-Client",
                null, // permanent
                Severity.HEAVY
        ));
        list.add(new Preset(
                "xray",
                "X-Ray / unfaire Client-Modifikation",
                days(7),
                Severity.HEAVY
        ));

        // Netzwerk / Sicherheit
        list.add(new Preset(
                "banumgehung",
                "Banumgehung (Zweitaccount)",
                null,
                Severity.HEAVY
        ));
        list.add(new Preset(
                "ddos",
                "Drohungen / DDoS / RL-Bedrohung",
                null,
                Severity.HEAVY
        ));

        // Sonstiges
        list.add(new Preset(
                "reportmissbrauch",
                "Report-System-Missbrauch",
                minutes(45),
                Severity.LIGHT
        ));
        list.add(new Preset(
                "nickmissbrauch",
                "Nick-/Identitätsmissbrauch",
                days(1),
                Severity.MEDIUM
        ));

        PRESETS = Collections.unmodifiableList(list);
    }

    private ReasonPresets() {
    }

    // ============================================================
    // HELFER FÜR DAUER
    // ============================================================

    private static long minutes(long m) {
        return m * 60_000L;
    }

    private static long hours(long h) {
        return h * 3_600_000L;
    }

    @SuppressWarnings("unused")
    private static long days(long d) {
        return d * 86_400_000L;
    }

    // ============================================================
    // API
    // ============================================================

    /**
     * Liefert ein Preset anhand des Keys (case-insensitive),
     * oder null, wenn nichts gefunden wurde.
     */
    public static Preset find(String key) {
        if (key == null || key.isBlank()) return null;
        String k = key.toLowerCase(Locale.ROOT);

        for (Preset p : PRESETS) {
            if (p.key().equalsIgnoreCase(k)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Tab-Completion über Keys.
     *
     * @param input bereits eingegebener Text
     * @return Liste der passenden Keys
     */
    public static List<String> tabComplete(String input) {
        String prefix = (input == null ? "" : input).toLowerCase(Locale.ROOT);

        List<String> out = new ArrayList<>();
        for (Preset p : PRESETS) {
            String key = p.key().toLowerCase(Locale.ROOT);
            if (prefix.isEmpty() || key.startsWith(prefix)) {
                out.add(p.key());
            }
        }
        return out;
    }

    /**
     * Zugriff auf alle Presets (read-only).
     */
    public static List<Preset> all() {
        return PRESETS;
    }
    public static List<String> allKeys() {
        return PRESETS.stream().map(Preset::key).toList();
    }

}
