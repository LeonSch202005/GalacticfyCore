package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import de.galacticfy.core.permission.GalacticfyRole;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RankCommand implements SimpleCommand {

    private final GalacticfyPermissionService perms;
    private final ProxyServer proxy;

    public RankCommand(GalacticfyPermissionService perms, ProxyServer proxy) {
        this.perms = perms;
        this.proxy = proxy;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    /**
     * Darf diese Source /rank benutzen?
     * - Konsole: immer true
     * - Spieler: nur, wenn seine Gruppe in DEINEM System
     *            die Permission "galacticfy.rank.admin" hat.
     */
    private boolean canUse(CommandSource src) {
        if (!(src instanceof Player player)) {
            return true; // Konsole
        }
        return perms.hasRankPermission(player, "galacticfy.rank.admin");
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!canUse(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        if (args.length == 0) {
            sendMainUsage(src);
            return;
        }

        String first = args[0].toLowerCase(Locale.ROOT);

        if (first.equals("group") || first.equals("g")) {
            handleGroup(src, args);
            return;
        }

        if (first.equals("user") || first.equals("u")) {
            handleUser(src, args);
            return;
        }

        sendMainUsage(src);
    }

    private void sendMainUsage(CommandSource src) {
        src.sendMessage(prefix().append(Component.text("§bRank-System Verwaltung")));
        src.sendMessage(Component.text("§8» §b/rank group list"));
        src.sendMessage(Component.text("§8» §b/rank group create <name> <display> [prio]"));
        src.sendMessage(Component.text("§8» §b/rank group delete <name>"));
        src.sendMessage(Component.text("§8» §b/rank group info <name>"));
        src.sendMessage(Component.text("§8» §b/rank group <name> permissions"));
        src.sendMessage(Component.text("§8» §b/rank group <name> set permission <node>"));
        src.sendMessage(Component.text("§8» §b/rank group <name> unset permission <node>"));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8» §b/rank user <Spieler> set <Gruppe>"));
        src.sendMessage(Component.text("§8» §b/rank user <Spieler> unset <Gruppe>"));
    }

    // ========================================================
    //  GROUP HANDLING
    // ========================================================

    private void handleGroup(CommandSource src, String[] args) {
        if (args.length == 1) {
            sendMainUsage(src);
            return;
        }

        String second = args[1].toLowerCase(Locale.ROOT);

        if (second.equals("list")) {
            handleGroupList(src);
            return;
        }
        if (second.equals("create")) {
            handleGroupCreate(src, args);
            return;
        }
        if (second.equals("delete")) {
            handleGroupDelete(src, args);
            return;
        }
        if (second.equals("info")) {
            handleGroupInfo(src, args);
            return;
        }

        String groupName = args[1];

        if (args.length == 3 && args[2].equalsIgnoreCase("permissions")) {
            handleGroupPermissionsList(src, groupName);
            return;
        }

        if (args.length >= 5 && args[2].equalsIgnoreCase("set")
                && args[3].equalsIgnoreCase("permission")) {
            String node = args[4];
            handleGroupAddPermission(src, groupName, node);
            return;
        }

        if (args.length >= 5 && args[2].equalsIgnoreCase("unset")
                && args[3].equalsIgnoreCase("permission")) {
            String node = args[4];
            handleGroupRemovePermission(src, groupName, node);
            return;
        }

        sendMainUsage(src);
    }

    private void handleGroupList(CommandSource src) {
        List<String> roles = perms.getAllRoleNames();
        if (roles.isEmpty()) {
            src.sendMessage(prefix().append(Component.text("§7Es sind noch keine Gruppen definiert.")));
            return;
        }
        src.sendMessage(prefix().append(Component.text(
                "§7Gruppen: §b" + String.join("§7, §b", roles)
        )));
    }

    // /rank group create <name> <display> [prio]
    private void handleGroupCreate(CommandSource src, String[] args) {
        if (args.length < 4) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: /rank group create <name> <display> [prio]"
            )));
            return;
        }

        String name = args[2].toLowerCase(Locale.ROOT);
        String display = args[3];

        int prio = 0;
        if (args.length >= 5) {
            try {
                prio = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                src.sendMessage(prefix().append(Component.text("§cPrio muss eine Zahl sein.")));
                return;
            }
        }

        String colorHex = "FFFFFF";
        String prefixStr = "§7[" + display + "§7]";

        boolean ok = perms.createRole(name, display, colorHex, prefixStr, false, false, prio);
        if (ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§aGruppe §b" + name + " §aerstellt."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§cKonnte Gruppe nicht erstellen (existiert sie?)."
            )));
        }
    }

    private void handleGroupDelete(CommandSource src, String[] args) {
        if (args.length < 3) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: /rank group delete <name>"
            )));
            return;
        }

        String name = args[2];
        boolean ok = perms.deleteRole(name);
        if (ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§aGruppe §b" + name + " §agelöscht."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§cKonnte Gruppe nicht löschen (existiert sie?)."
            )));
        }
    }

    private void handleGroupInfo(CommandSource src, String[] args) {
        if (args.length < 3) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: /rank group info <name>"
            )));
            return;
        }

        String name = args[2];
        GalacticfyRole role = perms.getRoleByName(name);
        if (role == null) {
            src.sendMessage(prefix().append(Component.text(
                    "§cGruppe §b" + name + " §cwurde nicht gefunden."
            )));
            return;
        }

        src.sendMessage(prefix().append(Component.text("§bGruppen-Info: §f" + role.name)));
        src.sendMessage(Component.text("§7Display: §f" + role.displayName));
        src.sendMessage(Component.text("§7Prefix: §f" + (role.prefix == null ? "§8<none>" : role.prefix)));
        src.sendMessage(Component.text("§7Farbe: §#" + (role.colorHex == null ? "FFFFFF" : role.colorHex)));
        src.sendMessage(Component.text("§7Prio: §e" + role.joinPriority));
    }

    private void handleGroupPermissionsList(CommandSource src, String groupName) {
        List<String> permsList = perms.getPermissionsOfRole(groupName);
        if (permsList.isEmpty()) {
            src.sendMessage(prefix().append(Component.text(
                    "§7Gruppe §b" + groupName + " §7hat aktuell keine Permissions."
            )));
            return;
        }

        String joined = String.join("§7, §b", permsList);
        src.sendMessage(prefix().append(Component.text(
                "§7Permissions von §b" + groupName + "§7: §b" + joined
        )));
    }

    private void handleGroupAddPermission(CommandSource src, String groupName, String node) {
        boolean ok = perms.addPermissionToRole(groupName, node);
        if (ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§aPermission §b" + node + " §awurde zu Gruppe §b" + groupName + " §ahinzugefügt."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§cKonnte Permission nicht hinzufügen."
            )));
        }
    }

    private void handleGroupRemovePermission(CommandSource src, String groupName, String node) {
        boolean ok = perms.removePermissionFromRole(groupName, node);
        if (ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§aPermission §b" + node + " §awurde von Gruppe §b" + groupName + " §aentfernt."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§cKonnte Permission nicht entfernen."
            )));
        }
    }

    // ========================================================
    //  USER HANDLING  (/rank user <name> set/unset <group>)
    // ========================================================

    private void handleUser(CommandSource src, String[] args) {
        if (args.length < 4) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: /rank user <Spieler> set <Gruppe> / unset <Gruppe>"
            )));
            return;
        }

        String playerName = args[1];
        String action = args[2].toLowerCase(Locale.ROOT);

        Player target = proxy.getPlayer(playerName).orElse(null);
        if (target == null) {
            src.sendMessage(prefix().append(Component.text(
                    "§cSpieler §b" + playerName + " §cist nicht online."
            )));
            return;
        }

        if (action.equals("set")) {
            String groupName = args[3].toLowerCase(Locale.ROOT);

            GalacticfyRole role = perms.getRoleByName(groupName);
            if (role == null) {
                src.sendMessage(prefix().append(Component.text(
                        "§cGruppe §b" + groupName + " §cwurde nicht gefunden."
                )));
                return;
            }

            boolean ok = perms.setRoleFor(target.getUniqueId(), target.getUsername(), groupName);
            if (ok) {
                src.sendMessage(prefix().append(Component.text(
                        "§aSpieler §b" + target.getUsername() + " §ahat nun Gruppe §b" + groupName + "§a."
                )));
                target.sendMessage(prefix().append(Component.text(
                        "§7Deine Gruppe wurde zu §b" + groupName + " §7geändert."
                )));
            } else {
                src.sendMessage(prefix().append(Component.text(
                        "§cKonnte Gruppe für den Spieler nicht setzen."
                )));
            }
            return;
        }

        if (action.equals("unset")) {
            String groupName = args[3].toLowerCase(Locale.ROOT);

            GalacticfyRole current = perms.getRoleFor(target.getUniqueId());
            String currentName = current != null
                    ? current.name.toLowerCase(Locale.ROOT)
                    : perms.getDefaultRoleName().toLowerCase(Locale.ROOT);

            if (!currentName.equals(groupName)) {
                src.sendMessage(prefix().append(Component.text(
                        "§cDer Spieler hat aktuell nicht die Gruppe §b" + groupName + "§c."
                )));
                return;
            }

            boolean ok = perms.setRoleToDefault(target.getUniqueId(), target.getUsername());
            if (ok) {
                String def = perms.getDefaultRoleName();
                src.sendMessage(prefix().append(Component.text(
                        "§aGruppe von §b" + target.getUsername() + " §awurde auf §b" + def + " §azurückgesetzt."
                )));
                target.sendMessage(prefix().append(Component.text(
                        "§7Deine Gruppe wurde auf §b" + def + " §7zurückgesetzt."
                )));
            } else {
                src.sendMessage(prefix().append(Component.text(
                        "§cKonnte Gruppe nicht zurücksetzen."
                )));
            }
            return;
        }

        src.sendMessage(prefix().append(Component.text(
                "§cBenutzung: /rank user <Spieler> set <Gruppe> / unset <Gruppe>"
        )));
    }

    // ========================================================
    //  Sichtbarkeit & Tab-Complete
    // ========================================================

    @Override
    public boolean hasPermission(Invocation invocation) {
        // steuert, ob der Command überhaupt im Tab auftaucht
        return canUse(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!canUse(src)) {
            return List.of();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("group", "user").stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }

        // /rank group ...
        if (args[0].equalsIgnoreCase("group") || args[0].equalsIgnoreCase("g")) {

            if (args.length == 2) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return List.of("list", "create", "delete", "info").stream()
                        .filter(s -> s.startsWith(prefix))
                        .toList();
            }

            if (args.length == 3) {
                String sub = args[1].toLowerCase(Locale.ROOT);
                String prefix = args[2].toLowerCase(Locale.ROOT);

                if (sub.equals("delete") || sub.equals("info")) {
                    return perms.getAllRoleNames().stream()
                            .filter(r -> r.toLowerCase(Locale.ROOT).startsWith(prefix))
                            .collect(Collectors.toList());
                }

                return List.of("permissions", "set", "unset").stream()
                        .filter(s -> s.startsWith(prefix))
                        .toList();
            }

            if (args.length == 4) {
                String action = args[2].toLowerCase(Locale.ROOT);
                String prefix = args[3].toLowerCase(Locale.ROOT);

                if (action.equals("set") || action.equals("unset")) {
                    return List.of("permission").stream()
                            .filter(s -> s.startsWith(prefix))
                            .toList();
                }
            }

            return List.of();
        }

        // /rank user ...
        if (args[0].equalsIgnoreCase("user") || args[0].equalsIgnoreCase("u")) {

            if (args.length == 2) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return proxy.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .toList();
            }

            if (args.length == 3) {
                String prefix = args[2].toLowerCase(Locale.ROOT);
                return List.of("set", "unset").stream()
                        .filter(s -> s.startsWith(prefix))
                        .toList();
            }

            if (args.length == 4) {
                String prefix = args[3].toLowerCase(Locale.ROOT);
                return perms.getAllRoleNames().stream()
                        .filter(r -> r.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .toList();
            }

            return List.of();
        }

        return List.of();
    }
}
