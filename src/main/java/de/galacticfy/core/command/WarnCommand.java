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

public class WarnCommand implements SimpleCommand {

    private static final String PERM_WARN = "galacticfy.punish.warn";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;
    private final DiscordWebhookNotifier webhook;

    public WarnCommand(ProxyServer proxy,
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

    private boolean hasWarnPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_WARN);
            }
            return p.hasPermission(PERM_WARN);
        }
        return true;
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

        String keyOrReason = args[1].toLowerCase(Locale.ROOT);
        String reason;
        String presetKeyUsed = null;

        Preset preset = ReasonPresets.find(keyOrReason);
        if (preset != null) {
            presetKeyUsed = preset.key();
            if (args.length > 2) {
                String extra = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                reason = preset.display() + " §8(§7" + extra + "§8)";
            } else {
                reason = preset.display();
            }
        } else {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
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

        // Du brauchst in PunishmentService:
        // Punishment warnPlayer(UUID uuid, String name, String ip, String reason, String staffName)
        Punishment p = punishmentService.warnPlayer(uuid, storedName, ip, reason, staffName);
        if (p == null) {
            src.sendMessage(prefix().append(Component.text("§cKonnte Warn nicht speichern (DB-Fehler).")));
            return;
        }

        int warnCount = punishmentService.countWarns(uuid, storedName);

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.BIG_HEADER_WARN));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7Spieler: §f" + storedName));
        src.sendMessage(Component.text("§7Grund:  §f" + reason));
        if (presetKeyUsed != null) {
            src.sendMessage(Component.text("§7Preset: §b" + presetKeyUsed));
        }
        src.sendMessage(Component.text("§7Warns:  §e" + warnCount));
        src.sendMessage(Component.text("§7Von:    §f" + staffName));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));

        if (target != null) {
            target.sendMessage(Component.text(
                    "§c§lGalacticfy §8» §cDu wurdest verwarnt.\n" +
                            "§7Grund: §f" + reason + "\n" +
                            "§7Aktuelle Warns: §e" + warnCount + "\n" +
                            " \n" +
                            "§7Bitte halte dich an die Regeln. Weitere Verstöße können zu §cBans§7 führen."
            ));
        }

        if (webhook != null && webhook.isEnabled()) {
            webhook.sendWarn(p);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasWarnPermission(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasWarnPermission(invocation.source())) {
            return List.of();
        }

        String[] args = invocation.arguments();

        // /warn <Spieler>
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
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // /warn <Spieler> <Preset|Text...>
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return ReasonPresets.tabComplete(prefix);
        }

        return List.of();
    }
}
