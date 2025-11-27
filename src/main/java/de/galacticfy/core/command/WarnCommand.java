package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.ReasonPresets;
import de.galacticfy.core.punish.ReasonPresets.Preset;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class WarnCommand implements SimpleCommand {

    private static final String PERM_WARN = "galacticfy.punish.warn";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;

    public WarnCommand(ProxyServer proxy,
                       GalacticfyPermissionService perms,
                       PunishmentService punishmentService) {
        this.proxy = proxy;
        this.perms = perms;
        this.punishmentService = punishmentService;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasWarnPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_WARN);
            }
            return p.hasPermission(PERM_WARN);
        }
        return true; // Konsole
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasWarnPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 2) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/warn <Spieler> <Preset|Grund...>"
            )));
            return;
        }

        String targetName = args[0];
        String reasonToken = args[1];

        // ===========================
        // Grund + Preset auflösen
        // ===========================
        Long durationMs = null;           // optional, wenn du Warnungen ablaufen lassen willst
        String reason;
        String presetKeyUsed = null;

        Preset preset = ReasonPresets.find(reasonToken);
        if (preset != null) {
            durationMs = preset.defaultDurationMs(); // kann null sein
            presetKeyUsed = preset.key();

            if (args.length > 2) {
                String extra = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                reason = preset.display() + " §8(§7" + extra + "§8)";
            } else {
                reason = preset.display();
            }
        } else {
            // frei eingegebener Grund
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        // ===========================
        // Zielspieler ermitteln
        // ===========================
        Player target = proxy.getPlayer(targetName).orElse(null);

        UUID uuid = null;
        String storedName = targetName;
        String ip = null;

        if (target != null) {
            uuid = target.getUniqueId();
            storedName = target.getUsername();

            Object remoteObj = target.getRemoteAddress();
            if (remoteObj instanceof InetSocketAddress isa && isa.getAddress() != null) {
                ip = isa.getAddress().getHostAddress();
            }
        }

        String staffName = (src instanceof Player p) ? p.getUsername() : "Konsole";

        // ===========================
        // Warn speichern
        // (setzt voraus, dass du in PunishmentService warnPlayer + Typ WARN hast)
        // ===========================
        Punishment p = punishmentService.warnPlayer(uuid, storedName, ip, reason, staffName, durationMs);
        if (p == null) {
            src.sendMessage(prefix().append(Component.text("§cKonnte Warn nicht speichern (DB-Fehler).")));
            return;
        }

        // ===========================
        // Nachrichten
        // ===========================
        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§aSpieler §e" + storedName + " §awurde verwarnt.")));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text("§7Grund: §f" + reason));
        if (presetKeyUsed != null) {
            src.sendMessage(Component.text("§7Preset: §b" + presetKeyUsed));
        }
        if (p.expiresAt != null) {
            src.sendMessage(Component.text("§7Läuft ab: §e" + punishmentService.formatRemaining(p)));
        }
        src.sendMessage(Component.text("§7Von: §f" + staffName));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));

        if (target != null) {
            target.sendMessage(prefix().append(Component.text(
                    "§cDu hast eine Verwarnung erhalten.§7 Grund: §f" + reason
            )));
        }
    }

    // ===========================
    // TAB-COMPLETE
    // ===========================

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasWarnPermission(src)) {
            return List.of();
        }

        // /warn <Spieler>
        if (args.length == 0 || args.length == 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

            // bekannte Namen aus DB + online Spieler
            Set<String> result = new LinkedHashSet<>();
            result.addAll(punishmentService.findKnownNames(prefix, 30));

            proxy.getAllPlayers().forEach(p -> {
                String n = p.getUsername();
                if (n.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(n);
                }
            });

            return new ArrayList<>(result);
        }

        // /warn <Spieler> <Preset|Grund>
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);

            // alle Preset-Keys (z.B. spam, beleidigung, hackclient, ...)
            return ReasonPresets.tabComplete(prefix);
        }

        // ab freiem Grund keine Vorschläge mehr
        return List.of();
    }
}
