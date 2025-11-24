package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.service.MaintenanceService;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MaintenanceCommand implements SimpleCommand {

    private final MaintenanceService maintenanceService;
    private final ProxyServer proxy;

    public MaintenanceCommand(MaintenanceService maintenanceService, ProxyServer proxy) {
        this.maintenanceService = maintenanceService;
        this.proxy = proxy;
    }

    // Prefix für alle Nachrichten
    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!source.hasPermission("galacticfy.maintenance.toggle")) {
            source.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> handleOn(source, args);
            case "off" -> {
                maintenanceService.setMaintenanceEnabled(false);
                source.sendMessage(prefix().append(Component.text("§aMaintenance wurde §adeaktiviert§a.")));
                proxy.sendMessage(prefix().append(Component.text("§aDie Wartungsarbeiten wurden beendet.")));
            }
            case "toggle" -> {
                boolean now = !maintenanceService.isMaintenanceEnabled();
                maintenanceService.setMaintenanceEnabled(now);
                source.sendMessage(prefix().append(Component.text("§aMaintenance ist jetzt: " + (now ? "§cAN" : "§aAUS"))));
                if (now) {
                    proxy.sendMessage(prefix().append(Component.text("§eWartungsarbeiten wurden §caktiviert§e.")));
                } else {
                    proxy.sendMessage(prefix().append(Component.text("§aDie Wartungsarbeiten wurden beendet.")));
                }
            }
            case "status" -> {
                boolean enabled = maintenanceService.isMaintenanceEnabled();
                Long remaining = maintenanceService.getRemainingMillis();
                String base = "§7Maintenance-Status: " + (enabled ? "§cAN" : "§aAUS");
                if (enabled && remaining != null && remaining > 0) {
                    long seconds = remaining / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    long s = seconds % 60;
                    long m = minutes % 60;
                    long h = hours % 24;
                    base += String.format(" §8(§7Rest: §e%sd %sh %sm %ss§8)", days, h, m, s);
                }
                source.sendMessage(prefix().append(Component.text(base)));
            }
            default -> sendUsage(source);
        }
    }

    private void handleOn(CommandSource source, String[] args) {
        // Nur "/maintenance on"
        if (args.length == 1) {
            maintenanceService.setMaintenanceEnabled(true);
            source.sendMessage(prefix().append(Component.text("§aMaintenance wurde §caktiviert§a (ohne Zeitbegrenzung).")));
            proxy.sendMessage(prefix().append(Component.text("§eWartungsarbeiten wurden §csofort gestartet§e.")));
            return;
        }

        // "/maintenance on time ..."
        if (args.length >= 3 && args[1].equalsIgnoreCase("time")) {
            List<String> durationTokens = new ArrayList<>();
            List<String> delayTokens = new ArrayList<>();

            boolean inDelayPart = false;
            for (int i = 2; i < args.length; i++) {
                String token = args[i];

                if (token.equalsIgnoreCase("start")) {
                    inDelayPart = true;
                    continue;
                }

                if (!inDelayPart) {
                    durationTokens.add(token);
                } else {
                    delayTokens.add(token);
                }
            }

            Long durationMs = parseDuration(durationTokens);
            if (durationMs == null) {
                source.sendMessage(prefix().append(Component.text("§cUngültige Zeitangabe bei der Dauer.")));
                return;
            }

            Long delayMs = null;
            if (!delayTokens.isEmpty()) {
                delayMs = parseDuration(delayTokens);
                if (delayMs == null) {
                    source.sendMessage(prefix().append(Component.text("§cUngültige Zeitangabe bei der Start-Verzögerung.")));
                    return;
                }
            }

            String durationFancy = formatDuration(durationMs);

            if (delayMs == null || delayMs <= 0) {
                // Sofort starten, mit Dauer
                maintenanceService.enableForDuration(durationMs, () ->
                        proxy.sendMessage(prefix().append(
                                Component.text("§eWartungsarbeiten wurden §cgestartet§e und laufen §c" + durationFancy + "§e.")
                        ))
                );
                source.sendMessage(prefix().append(Component.text("§aMaintenance wurde §caktiviert§a für §e" +
                        durationFancy + "§a.")));
            } else {
                String delayFancy = formatDuration(delayMs);

                // geplante Maintenance: Broadcast JETZT "beginnt in ..."
                proxy.sendMessage(prefix().append(
                        Component.text("§eWartungsarbeiten beginnen in §c" + delayFancy +
                                " §eund laufen dann §c" + durationFancy + "§e.")
                ));

                // und beim Start noch einmal Broadcast
                maintenanceService.scheduleMaintenance(delayMs, durationMs, () ->
                        proxy.sendMessage(prefix().append(
                                Component.text("§eWartungsarbeiten §cstarten jetzt §eund laufen §c" +
                                        durationFancy + "§e.")
                        ))
                );

                source.sendMessage(prefix().append(Component.text(
                        "§aMaintenance wird in §e" + delayFancy +
                                " §agestartet und läuft dann §e" + durationFancy + "§a."
                )));
            }
            return;
        }

        sendUsage(source);
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(prefix().append(Component.text("§eBenutzung:")));
        source.sendMessage(Component.text("§e/maintenance on"));
        source.sendMessage(Component.text("§e/maintenance off"));
        source.sendMessage(Component.text("§e/maintenance toggle"));
        source.sendMessage(Component.text("§e/maintenance status"));
        source.sendMessage(Component.text("§e/maintenance on time <Dauer>"));
        source.sendMessage(Component.text("§e/maintenance on time <Dauer> start <Verzögerung>"));
        source.sendMessage(Component.text("§7Beispiele:"));
        source.sendMessage(Component.text("§7/maintenance on time 30m"));
        source.sendMessage(Component.text("§7/maintenance on time 1d 2h 30m start 10m"));
    }

    // --- Zeit-Parsing / Formatierung ----------------------------------------------------

    private Long parseDuration(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return null;

        long totalMs = 0L;

        for (String raw : tokens) {
            if (raw == null || raw.isEmpty()) continue;

            String s = raw.toLowerCase(Locale.ROOT).trim();
            char unit = s.charAt(s.length() - 1);
            String numberPart = s.substring(0, s.length() - 1);

            long value;
            try {
                value = Long.parseLong(numberPart);
            } catch (NumberFormatException e) {
                return null;
            }

            long factor;
            switch (unit) {
                case 'j': // Tage (alternative Schreibweise)
                case 'd':
                    factor = 24L * 60L * 60L * 1000L;
                    break;
                case 'h':
                    factor = 60L * 60L * 1000L;
                    break;
                case 'm':
                    factor = 60L * 1000L;
                    break;
                case 's':
                    factor = 1000L;
                    break;
                default:
                    return null;
            }

            totalMs += value * factor;
        }

        if (totalMs <= 0) return null;
        return totalMs;
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        long s = seconds % 60;
        long m = minutes % 60;
        long h = hours % 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("s");

        return sb.toString().trim();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("galacticfy.maintenance.toggle");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission("galacticfy.maintenance.toggle")) {
            return List.of();
        }

        String[] args = invocation.arguments();

        List<String> baseSubs = List.of("on", "off", "toggle", "status");

        if (args.length == 0) {
            return baseSubs;
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if (prefix.isEmpty()) {
                return baseSubs;
            }
            return baseSubs.stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .toList();
        }

        if (!args[0].equalsIgnoreCase("on")) {
            return List.of();
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> opts = List.of("time");
            if (prefix.isEmpty()) {
                return opts;
            }
            return opts.stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .toList();
        }

        if (!args[1].equalsIgnoreCase("time")) {
            return List.of();
        }

        List<String> durationExamples = List.of(
                "30s", "1m", "5m", "10m", "30m",
                "1h", "2h", "6h", "12h",
                "1d", "2d"
        );

        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        boolean hasStart = Arrays.stream(args).anyMatch(a -> a.equalsIgnoreCase("start"));
        int startIndex = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("start")) {
                startIndex = i;
                break;
            }
        }

        if (!hasStart) {
            List<String> suggestions = new ArrayList<>(durationExamples);
            suggestions.add("start");

            if (last.isEmpty()) {
                return suggestions;
            }

            return suggestions.stream()
                    .filter(opt -> opt.toLowerCase(Locale.ROOT).startsWith(last))
                    .toList();
        }

        if (startIndex == args.length - 1) {
            return durationExamples;
        }

        if (args.length - 1 > startIndex) {
            String prefix = last;
            if (prefix.isEmpty()) {
                return durationExamples;
            }
            return durationExamples.stream()
                    .filter(opt -> opt.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }

        return List.of();
    }
}
