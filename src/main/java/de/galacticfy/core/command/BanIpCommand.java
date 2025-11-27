package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.punish.PunishDesign;
import de.galacticfy.core.service.PunishmentService;
import de.galacticfy.core.service.PunishmentService.Punishment;
import de.galacticfy.core.util.DiscordWebhookNotifier;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class BanIpCommand implements SimpleCommand {

    private static final String PERM_BANIP = "galacticfy.punish.banip";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;
    private final DiscordWebhookNotifier webhook;

    public BanIpCommand(ProxyServer proxy,
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

    private boolean hasBanIpPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_BANIP);
            }
            return p.hasPermission(PERM_BANIP);
        }
        return true;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasBanIpPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length < 2) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/banip <IP> <Dauer|perm> [Grund...]"
            )));
            return;
        }

        String ip = args[0];

        if (!ip.contains(".")) {
            src.sendMessage(prefix().append(Component.text("§cBitte eine gültige IP angeben.")));
            return;
        }

        String durationArg = args[1].toLowerCase(Locale.ROOT);
        Long durationMs;
        String reason;

        if (durationArg.equals("perm") || durationArg.equals("permanent")) {
            durationMs = null;
        } else {
            durationMs = punishmentService.parseDuration(durationArg);
            if (durationMs == null) {
                src.sendMessage(prefix().append(Component.text(
                        "§cUngültige Dauer! Beispiele: §b30m§7, §b1h§7, §b7d§7, §bperm§7"
                )));
                return;
            }
        }

        if (args.length > 2) {
            reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        } else {
            reason = "Kein Grund angegeben";
        }

        String staffName = (src instanceof Player p) ? p.getUsername() : "Konsole";

        Punishment p = punishmentService.banIp(ip, reason, staffName, durationMs);
        if (p == null) {
            src.sendMessage(prefix().append(Component.text("§cKonnte IP-Ban nicht speichern (DB-Fehler).")));
            return;
        }

        String durText = (p.expiresAt == null)
                ? "§cPermanent"
                : "§e" + punishmentService.formatRemaining(p);

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text(PunishDesign.BIG_HEADER_BANIP));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7IP:     §f" + ip));
        src.sendMessage(Component.text("§7Grund:  §f" + reason));
        src.sendMessage(Component.text("§7Dauer:  " + durText));
        src.sendMessage(Component.text("§7Von:    §f" + staffName));
        src.sendMessage(Component.text(PunishDesign.LINE));
        src.sendMessage(Component.text(" "));

        Component kickMsg = Component.text(
                "§c§lGalacticfy §8» §cDu wurdest (IP) gebannt.\n" +
                        "§7IP: §f" + ip + "\n" +
                        "§7Grund: §f" + reason + "\n" +
                        "§7Dauer: " + (p.expiresAt == null ? "§cPermanent" : "§e" + punishmentService.formatRemaining(p)) + "\n" +
                        "§7Von: §b" + staffName
        );

        proxy.getAllPlayers().forEach(player -> {
            Object remote = player.getRemoteAddress();
            if (remote instanceof InetSocketAddress isa && isa.getAddress() != null) {
                String playerIp = isa.getAddress().getHostAddress();
                if (ip.equals(playerIp)) {
                    player.disconnect(kickMsg);
                }
            }
        });

        if (webhook != null && webhook.isEnabled()) {
            webhook.sendBan(p);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasBanIpPermission(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasBanIpPermission(invocation.source())) {
            return List.of();
        }

        String[] args = invocation.arguments();

        // /banip <IP>
        if (args.length == 1 || args.length == 0) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

            return proxy.getAllPlayers().stream()
                    .map(player -> {
                        Object remote = player.getRemoteAddress();
                        if (remote instanceof InetSocketAddress isa && isa.getAddress() != null) {
                            return isa.getAddress().getHostAddress();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .filter(ip -> ip.startsWith(prefix))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        // /banip <IP> <Dauer>
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> base = Arrays.asList("10m", "30m", "1h", "6h", "1d", "7d", "30d", "1mo", "1y", "perm");
            return base.stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
