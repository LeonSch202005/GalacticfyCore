package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class StaffChatCommand implements SimpleCommand {

    private static final String PERM_STAFFCHAT_WRITE = "galacticfy.staffchat.write";
    private static final String PERM_STAFFCHAT_READ  = "galacticfy.staffchat.read";

    private final ProxyServer proxy;
    private final GalacticfyPermissionService perms;

    public StaffChatCommand(ProxyServer proxy,
                            GalacticfyPermissionService perms) {
        this.proxy = proxy;
        this.perms = perms;
    }

    private Component prefix() {
        return Component.text("§8[§cStaff§8] §r");
    }

    private boolean canWrite(CommandSource src) {
        if (src instanceof Player p) {
            if (perms != null) {
                return perms.hasPluginPermission(p, PERM_STAFFCHAT_WRITE);
            }
            return p.hasPermission(PERM_STAFFCHAT_WRITE);
        }
        return true;
    }

    private boolean canRead(Player p) {
        if (perms != null) {
            return perms.hasPluginPermission(p, PERM_STAFFCHAT_READ)
                    || perms.hasPluginPermission(p, PERM_STAFFCHAT_WRITE);
        }
        return p.hasPermission(PERM_STAFFCHAT_READ) || p.hasPermission(PERM_STAFFCHAT_WRITE);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!canWrite(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length == 0) {
            src.sendMessage(prefix().append(Component.text(
                    "§eBenutzung: §b/staffchat <Nachricht...>"
            )));
            return;
        }

        String msg = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        String name = (src instanceof Player p) ? p.getUsername() : "Konsole";

        Component out = Component.text("§8[§cStaff§8] §c" + name + "§8: §7" + msg);

        // an Console
        proxy.getConsoleCommandSource().sendMessage(out);

        // an Staff
        proxy.getAllPlayers().forEach(p -> {
            if (canRead(p)) {
                p.sendMessage(out);
            }
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        // keine speziellen Vorschläge
        return List.of();
    }
}
