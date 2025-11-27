package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.PunishDesign;
import de.galacticfy.core.punish.ReasonPresets;
import de.galacticfy.core.punish.ReasonPresets.Preset;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import de.galacticfy.core.util.DiscordWebhookNotifier;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class MuteCommand implements SimpleCommand {

    private static final String PERM_MUTE = "galacticfy.punish.mute";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;
    private final DiscordWebhookNotifier webhook;

    public MuteCommand(ProxyServer proxy,
                       GalacticfyPermissionService perms,
                       PunishmentService punishmentService,
                       DiscordWebhookNotifier webhook) {
        this.proxy = proxy;
        this.perms = perms;
        this.punishmentService = punishmentService;
        this.webhook = webhook;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean hasMutePermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_MUTE);
            }
            return p.hasPermission(PERM_MUTE);
        }
        return true;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasMutePermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 2) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/mute <Spieler> <Dauer|Preset|perm> [Grund-Extra...]"
            )));
            return;
        }

        String targetName = args[0];
        String durationOrPreset = args[1].toLowerCase(Locale.ROOT);

        Long durationMs;
        String reason;
        String presetKeyUsed = null;

        Preset preset = ReasonPresets.find(durationOrPreset);
        if (preset != null) {
            durationMs = preset.defaultDurationMs();
            presetKeyUsed = preset.key();

            if (args.length > 2) {
                String extra = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                reason = preset.display() + " §8(§7" + extra + "§8)";
            } else {
                reason = preset.display();
            }
        } else {
            if (durationOrPreset.equals("perm") || durationOrPreset.equals("permanent")) {
                durationMs = null;
            } else {
                durationMs = punishmentService.parseDuration(durationOrPreset);
                if (durationMs == null) {
                    src.sendMessage(prefix().append(Component.text(
                            "§cUngültige Dauer oder Preset! Beispiele: §b30m§7, §b1h§7, §b7d§7, §bspam§7, §bperm§7"
                    )));
                    return;
                }
            }

            if (args.length > 2) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            } else {
                reason = "Kein Grund angegeben";
            }
        }

        Player target = proxy.getPlayer(targetName).orElse(null);

        UUID uuid = null;
        String storedName = targetName;
        String ip = null;

        if (target != null) {
            uuid = target.getUniqueId();
            storedName = target.getUsername();

            Object remote = target.getRemoteAddress();
            if (remote instanceof InetSocketAddress isa && isa.getAddress() != null) {
                ip = isa.getAddress().getHostAddress();
            }
        }

        String staffName = (src instanceof Player p) ? p.getUsername() : "Konsole";

        Punishment p = punishmentService.mutePlayer(uuid, storedName, ip, reason, staffName, durationMs);
        if (p == null) {
            src.sendMessage(prefix().append(Component.text("§cKonnte Mute nicht speichern (DB-Fehler).")));
            return;
        }

        String durText = (p.expiresAt == null)
                ? "§cPermanent"
                : "§e" + punishmentService.formatRemaining(p);

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.BIG_HEADER_MUTE));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7Spieler: §f" + storedName));
        src.sendMessage(Component.text("§7Grund:  §f" + reason));
        src.sendMessage(Component.text("§7Dauer:  " + durText));
        if (presetKeyUsed != null) {
            src.sendMessage(Component.text("§7Preset: §b" + presetKeyUsed));
        }
        src.sendMessage(Component.text("§7Von:    §f" + staffName));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));

        if (target != null) {
            String remaining = (p.expiresAt == null)
                    ? "§cPermanent"
                    : "§e" + punishmentService.formatRemaining(p);

            target.sendMessage(Component.text(
                    "§c§lGalacticfy §8» §cDu wurdest gemutet.\n" +
                            "§7Grund: §f" + reason + "\n" +
                            "§7Dauer: " + remaining + "\n" +
                            " \n" +
                            "§7Du kannst weiterhin §bCommands §7nutzen, aber nicht schreiben."
            ));
        }

        if (webhook != null && webhook.isEnabled()) {
            webhook.sendMute(p);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasMutePermission(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasMutePermission(invocation.source())) {
            return List.of();
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> prefix.isEmpty()
                            || name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);

            List<String> base = Arrays.asList(
                    "10m", "30m", "1h", "6h", "1d", "7d", "30d", "1mo", "1y", "perm"
            );

            List<String> presets = ReasonPresets.allKeys();

            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(base);
            suggestions.addAll(presets);

            return suggestions.stream()
                    .filter(s -> prefix.isEmpty()
                            || s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
