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
import de.galacticfy.core.util.DiscordWebhookNotifier;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class KickCommand implements SimpleCommand {

    private static final String PERM_KICK = "galacticfy.punish.kick";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;
    private final PunishmentService punishmentService;
    private final DiscordWebhookNotifier webhook;

    public KickCommand(ProxyServer proxy,
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

    private boolean hasKickPermission(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_KICK);
            }
            return p.hasPermission(PERM_KICK);
        }
        return true;
    }

    @Override
    public void execute(Invocation invocation) {

        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!hasKickPermission(src)) {
            src.sendMessage(prefix().append(Component.text("§cKeine Berechtigung.")));
            return;
        }

        if (args.length < 1) {
            src.sendMessage(prefix().append(Component.text("§eBenutzung: §b/kick <Spieler> [Grund/Preset]")));
            return;
        }

        String targetName = args[0];
        String reason;
        String extra = null;

        // ============================================
        // Grund + Presets
        // ============================================

        if (args.length == 1) {
            reason = "Kein Grund angegeben";
        } else {
            String key = args[1].toLowerCase(Locale.ROOT);

            Preset preset = ReasonPresets.find(key);

            if (preset != null) {
                if (args.length > 2) {
                    extra = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    reason = preset.display() + " §8(§7" + extra + "§8)";
                } else {
                    reason = preset.display();
                }
            } else {
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }

        Player target = proxy.getPlayer(targetName).orElse(null);

        if (target == null) {
            src.sendMessage(prefix().append(Component.text("§cSpieler ist nicht online.")));
            return;
        }

        UUID uuid = target.getUniqueId();
        String storedName = target.getUsername();
        String ip = null;

        Object remote = target.getRemoteAddress();
        if (remote instanceof InetSocketAddress isa && isa.getAddress() != null) {
            ip = isa.getAddress().getHostAddress();
        }

        String staffName = (src instanceof Player p) ? p.getUsername() : "Konsole";

        // ============================================
        // Kick in DB speichern
        // ============================================

        Punishment p = punishmentService.logKick(uuid, storedName, ip, reason, staffName);

        // Kick-Nachricht
        Component msg = Component.text(
                "§cDu wurdest gekickt.\n" +
                        "§7Grund: §e" + reason + "\n" +
                        "§7Von: §b" + staffName
        );

        target.disconnect(msg);

        // Staff-Feedback
        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§a" + storedName + " §awurde gekickt.")));
        src.sendMessage(Component.text("§7Grund: §f" + reason));
        src.sendMessage(Component.text("§7Von: §f" + staffName));
        src.sendMessage(Component.text(" "));

        // Discord Webhook
        if (webhook != null && webhook.isEnabled() && p != null) {
            webhook.sendKick(p);
        }
    }

    // ============================================
    // TAB COMPLETE
    // ============================================

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        // /kick <TAB>  → alle Spieler
        if (args.length == 0) {
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .sorted()
                    .collect(Collectors.toList());
        }

        // /kick <Spieler> [TAB] → Spieler nach Prefix filtern
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // /kick <Spieler> <Grund/Preset> [TAB]
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            // zeigt alle Presets, gefiltert nach Prefix (oder alle, wenn prefix leer)
            return ReasonPresets.tabComplete(prefix);
        }

        // ab drittem Argument kein Tab nötig
        return List.of();
    }

}
