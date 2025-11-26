package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.permission.GalacticfyPermissionService;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RankCommand implements SimpleCommand {

    private static final String ADMIN_PERMISSION = "galacticfy.rank.admin";

    private final GalacticfyPermissionService perms;
    private final ProxyServer proxy;

    public RankCommand(GalacticfyPermissionService perms, ProxyServer proxy) {
        this.perms = perms;
        this.proxy = proxy;
    }

    private Component prefix() {
        return Component.text("§8[§bGalacticfy§8] §r");
    }

    private boolean isRankAdmin(CommandSource src) {
        if (perms == null) {
            // falls aus irgendeinem Grund nicht initialisiert → Konsole ok, Spieler nur normale Permission
            if (src instanceof Player player) {
                return player.hasPermission(ADMIN_PERMISSION);
            }
            return true;
        }

        return perms.hasPluginPermission(src, ADMIN_PERMISSION);
    }


    // ============================================================
    // EXECUTE
    // ============================================================

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!isRankAdmin(src)) {
            src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
            return;
        }

        // Nur /rank → kurzer Überblick
        if (args.length == 0) {
            sendShortOverview(src);
            return;
        }

        String first = args[0].toLowerCase(Locale.ROOT);

        // /rank help
        if (first.equals("help")) {
            sendFullHelp(src);
            return;
        }

        switch (first) {
            case "group" -> handleGroup(src, args);
            case "user"  -> handleUser(src, args);
            default -> sendShortOverview(src);
        }
    }

    // ============================================================
    // GROUP SUBCOMMANDS
    // ============================================================

    private void handleGroup(CommandSource src, String[] args) {
        if (args.length == 1) {
            sendGroupUsage(src);
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        // /rank group list|create|delete|info|permissions|inherit|set ...
        switch (sub) {
            case "list" -> {
                handleGroupList(src);
                return;
            }
            case "create" -> {
                handleGroupCreate(src, args);
                return;
            }
            case "delete" -> {
                handleGroupDelete(src, args);
                return;
            }
            case "info" -> {
                handleGroupInfo(src, args);
                return;
            }
            case "permissions" -> {
                handleGroupPermissionsList(src, args);
                return;
            }
            case "inherit" -> {
                handleGroupInherit(src, args);
                return;
            }
            case "set" -> {
                handleGroupSetMeta(src, args);
                return;
            }
        }

        // Ab hier: /rank group <name> ...
        String groupName = args[1];

        // /rank group <name>
        if (args.length == 2) {
            showGroupPermissions(src, groupName);
            return;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "permissions" -> showGroupPermissions(src, groupName);
            case "set" -> handleGroupSetPermission(src, groupName, args);
            case "unset" -> handleGroupUnsetPermission(src, groupName, args);
            default -> sendGroupUsage(src);
        }
    }

    private void handleGroupList(CommandSource src) {
        List<String> roles = perms.getAllRoleNames();
        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§bAlle Gruppen")));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        if (roles.isEmpty()) {
            src.sendMessage(Component.text("§7Es sind noch keine Gruppen definiert."));
        } else {
            src.sendMessage(Component.text("§7Gruppen: §b" + String.join("§7, §b", roles)));
        }
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    // /rank group create <name> <display> [prio]
    private void handleGroupCreate(CommandSource src, String[] args) {
        if (args.length < 4) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank group create <name> <display> [prio]"
            )));
            return;
        }

        String name = args[2];
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

        // Default-Werte
        String colorHex = "FFFFFF";
        String prefixStr = "";
        String suffixStr = "";
        boolean staff = false;
        boolean maintBypass = false;

        boolean ok = perms.createRole(name, display, colorHex, prefixStr, suffixStr, staff, maintBypass, prio);
        if (ok) {
            src.sendMessage(prefix().append(Component.text("§aGruppe §b" + name + " §aerstellt.")));
        } else {
            src.sendMessage(prefix().append(Component.text("§cKonnte Gruppe nicht erstellen (existiert sie schon?).")));
        }
    }

    // /rank group delete <name>
    private void handleGroupDelete(CommandSource src, String[] args) {
        if (args.length < 3) {
            src.sendMessage(prefix().append(Component.text("§cBenutzung: §b/rank group delete <name>")));
            return;
        }

        String name = args[2];
        boolean ok = perms.deleteRole(name);
        if (ok) {
            src.sendMessage(prefix().append(Component.text("§aGruppe §b" + name + " §agelöscht.")));
        } else {
            src.sendMessage(prefix().append(Component.text("§cKonnte Gruppe nicht löschen (existiert sie?).")));
        }
    }

    // /rank group info <name>
    private void handleGroupInfo(CommandSource src, String[] args) {
        if (args.length < 3) {
            src.sendMessage(prefix().append(Component.text("§cBenutzung: §b/rank group info <name>")));
            return;
        }

        String name = args[2];
        var role = perms.getRoleByName(name);
        if (role == null) {
            src.sendMessage(prefix().append(Component.text("§cGruppe §b" + name + " §cexistiert nicht.")));
            return;
        }

        List<String> parents = perms.getParentsOfRole(role.name);

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8§m──────§r §bGalacticfy §7| §bGruppe §f" + role.name + " §8§m──────"));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7Anzeige-Name: §f" + role.displayName));
        src.sendMessage(Component.text("§7Color-Hex: §f#" + role.colorHex));
        src.sendMessage(Component.text("§7Prefix: §f" + (role.prefix == null ? "" : role.prefix)));
        src.sendMessage(Component.text("§7Suffix: §f" + (role.suffix == null ? "" : role.suffix)));
        src.sendMessage(Component.text("§7Staff: §f" + role.staff));
        src.sendMessage(Component.text("§7Maintenance-Bypass: §f" + role.maintenanceBypass));
        src.sendMessage(Component.text("§7Priorität: §f" + role.joinPriority));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§bVererbte Gruppen:"));
        if (parents.isEmpty()) {
            src.sendMessage(Component.text("§7(keine Parents gesetzt)"));
        } else {
            src.sendMessage(Component.text("§7" + String.join("§7, §b", parents)));
        }

        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    // /rank group permissions <name>
    private void handleGroupPermissionsList(CommandSource src, String[] args) {
        if (args.length < 3) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank group permissions <name>"
            )));
            return;
        }
        String group = args[2];
        showGroupPermissions(src, group);
    }

    private void showGroupPermissions(CommandSource src, String groupName) {
        var role = perms.getRoleByName(groupName);
        if (role == null) {
            src.sendMessage(prefix().append(Component.text("§cGruppe §b" + groupName + " §cexistiert nicht.")));
            return;
        }

        List<String> permsList = perms.getPermissionsOfRole(role.name);
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8§m──────§r §bGalacticfy §7| §bPermissions §8§m──────"));
        src.sendMessage(prefix().append(Component.text("§bGruppe: §f" + role.name)));

        if (permsList.isEmpty()) {
            src.sendMessage(Component.text("§7(keine Einträge)"));
        } else {
            for (String p : permsList) {
                src.sendMessage(Component.text("§8- §f" + p));
            }
        }
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    // /rank group <name> set permission <node>
    private void handleGroupSetPermission(CommandSource src, String groupName, String[] args) {
        if (args.length < 5 || !"permission".equalsIgnoreCase(args[3])) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank group " + groupName + " set permission <node>"
            )));
            return;
        }

        String node = args[4];
        boolean ok = perms.addPermissionToRole(groupName, node);
        if (ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§aPermission §b" + node + " §azu Gruppe §b" + groupName + " §ahinzugefügt."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§cKonnte Permission nicht hinzufügen."
            )));
        }
    }

    // /rank group <name> unset permission <node>
    private void handleGroupUnsetPermission(CommandSource src, String groupName, String[] args) {
        if (args.length < 5 || !"permission".equalsIgnoreCase(args[3])) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank group " + groupName + " unset permission <node>"
            )));
            return;
        }

        String node = args[4];
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

    // ===== NEU: Meta-Set: /rank group set prefix/suffix ... =====

    /**
     * /rank group set prefix <Gruppe> <Prefix...>
     * /rank group set suffix <Gruppe> <Suffix...>
     */
    private void handleGroupSetMeta(CommandSource src, String[] args) {
        if (args.length < 5) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank group set prefix <Gruppe> <Prefix...>\n" +
                            "§coder:     §b/rank group set suffix <Gruppe> <Suffix...>"
            )));
            return;
        }

        String type = args[2].toLowerCase(Locale.ROOT);

        if (!type.equals("prefix") && !type.equals("suffix")) {
            src.sendMessage(prefix().append(Component.text(
                    "§cUnbekannter Typ §b" + type + "§c. Nutze §bprefix§c oder §bsuffix§c."
            )));
            return;
        }

        String groupName = args[3];

        // Prefix/Suffix kann Leerzeichen enthalten → Rest joinen
        StringBuilder sb = new StringBuilder();
        for (int i = 4; i < args.length; i++) {
            if (i > 4) sb.append(" ");
            sb.append(args[i]);
        }
        String value = sb.toString();

        boolean ok;
        if (type.equals("prefix")) {
            ok = perms.updateRolePrefix(groupName, value);
        } else {
            ok = perms.updateRoleSuffix(groupName, value);
        }

        if (ok) {
            src.sendMessage(prefix().append(Component.text(
                    "§a" + type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1)
                            + " §bder Gruppe §f" + groupName + " §awurde auf §f" + value + " §agesetzt."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§cKonnte " + type + " nicht setzen. Existiert die Gruppe?"
            )));
        }
    }

    // ===== Inherit: /rank group inherit ... =====================

    private void handleGroupInherit(CommandSource src, String[] args) {
        if (args.length < 3) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank group inherit list|add|remove ..."
            )));
            return;
        }

        String mode = args[2].toLowerCase(Locale.ROOT);

        switch (mode) {
            case "list" -> {
                if (args.length < 4) {
                    src.sendMessage(prefix().append(Component.text(
                            "§cBenutzung: §b/rank group inherit list <Gruppe>"
                    )));
                    return;
                }
                String group = args[3];
                List<String> parents = perms.getParentsOfRole(group);
                src.sendMessage(Component.text(" "));
                src.sendMessage(prefix().append(Component.text("§bInherit-Liste für §f" + group)));
                if (parents.isEmpty()) {
                    src.sendMessage(Component.text("§7(keine Parents gesetzt)"));
                } else {
                    src.sendMessage(Component.text("§7Parents: §b" + String.join("§7, §b", parents)));
                }
                src.sendMessage(Component.text(" "));
            }

            case "add" -> {
                if (args.length < 5) {
                    src.sendMessage(prefix().append(Component.text(
                            "§cBenutzung: §b/rank group inherit add <Gruppe> <Parent>"
                    )));
                    return;
                }
                String group = args[3];
                String parent = args[4];
                boolean ok = perms.addInheritedRole(group, parent);
                if (ok) {
                    src.sendMessage(prefix().append(Component.text(
                            "§aGruppe §b" + parent + " §awird nun von §b" + group + " §aererbt."
                    )));
                } else {
                    src.sendMessage(prefix().append(Component.text(
                            "§cKonnte Inherit nicht setzen. Prüfe, ob beide Gruppen existieren."
                    )));
                }
            }

            case "remove" -> {
                if (args.length < 5) {
                    src.sendMessage(prefix().append(Component.text(
                            "§cBenutzung: §b/rank group inherit remove <Gruppe> <Parent>"
                    )));
                    return;
                }
                String group = args[3];
                String parent = args[4];
                boolean ok = perms.removeInheritedRole(group, parent);
                if (ok) {
                    src.sendMessage(prefix().append(Component.text(
                            "§aInherit §b" + group + " §7-> §b" + parent + " §awurde entfernt."
                    )));
                } else {
                    src.sendMessage(prefix().append(Component.text(
                            "§cKonnte Inherit nicht entfernen."
                    )));
                }
            }

            default -> src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank group inherit list|add|remove ..."
            )));
        }
    }

    // ============================================================
    // USER SUBCOMMANDS
    // ============================================================

    private void handleUser(CommandSource src, String[] args) {
        if (args.length < 3) {
            sendUserUsage(src);
            return;
        }

        String playerName = args[1];
        String action = args[2].toLowerCase(Locale.ROOT);

        Player target = proxy.getPlayer(playerName).orElse(null);
        if (target == null) {
            src.sendMessage(prefix().append(Component.text("§cSpieler §b" + playerName + " §cist nicht online.")));
            return;
        }

        UUID uuid = target.getUniqueId();

        switch (action) {
            case "set" -> handleUserSetGroup(src, target, uuid, args);
            case "unset" -> handleUserUnsetGroup(src, target, uuid, args);
            default -> sendUserUsage(src);
        }
    }

    // /rank user <spieler> set group <gruppe> [Dauer]
    private void handleUserSetGroup(CommandSource src, Player target, UUID uuid, String[] args) {
        if (args.length < 5 || !"group".equalsIgnoreCase(args[3])) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank user <Spieler> set group <Gruppe> [Dauer]"
            )));
            return;
        }

        String groupName = args[4];
        Long durationMs = null;

        // Optional: Dauer erkannt?
        if (args.length >= 6) {
            durationMs = parseDuration(args[5]);
            if (durationMs == null) {
                src.sendMessage(prefix().append(Component.text(
                        "§cUngültiges Dauer-Format! Beispiele: §b7d§7, §b3h§7, §b30m§7, §b1mo§7"
                )));
                return;
            }
        }

        boolean ok;

        if (durationMs != null) {
            ok = perms.setRoleForDuration(uuid, target.getUsername(), groupName, durationMs);
        } else {
            ok = perms.setRoleFor(uuid, target.getUsername(), groupName);
        }

        if (ok) {
            if (durationMs != null) {
                src.sendMessage(prefix().append(Component.text(
                        "§aSpieler §b" + target.getUsername() +
                                " §ahat nun Gruppe §b" + groupName +
                                " §afür §e" + args[5] + "§a."
                )));
                target.sendMessage(prefix().append(Component.text(
                        "§7Dein Rang wurde zu §b" + groupName +
                                " §7gesetzt (Dauer: §e" + args[5] + "§7)."
                )));
            } else {
                src.sendMessage(prefix().append(Component.text(
                        "§aSpieler §b" + target.getUsername() +
                                " §ahat nun Gruppe §b" + groupName + "§a."
                )));
                target.sendMessage(prefix().append(Component.text(
                        "§7Deine Gruppe wurde zu §b" + groupName + " §7geändert."
                )));
            }

        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§cKonnte Gruppe nicht setzen (existiert sie?)."
            )));
        }
    }

    // /rank user <spieler> unset group
    private void handleUserUnsetGroup(CommandSource src, Player target, UUID uuid, String[] args) {
        if (args.length >= 4 && !"group".equalsIgnoreCase(args[3])) {
            src.sendMessage(prefix().append(Component.text(
                    "§cBenutzung: §b/rank user <Spieler> unset group"
            )));
            return;
        }

        boolean ok = perms.setRoleToDefault(uuid, target.getUsername());
        if (ok) {
            String defName = perms.getDefaultRoleName();
            src.sendMessage(prefix().append(Component.text(
                    "§aSpieler §b" + target.getUsername() + " §awurde auf Standard-Gruppe §b" + defName + " §azurückgesetzt."
            )));
            target.sendMessage(prefix().append(Component.text(
                    "§7Deine Gruppe wurde auf §b" + defName + " §7zurückgesetzt."
            )));
        } else {
            src.sendMessage(prefix().append(Component.text(
                    "§cKonnte Gruppe nicht zurücksetzen."
            )));
        }
    }

    // ============================================================
    // HELP / USAGE
    // ============================================================

    private void sendShortOverview(CommandSource src) {
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8§m──────§r §bGalacticfy §7| §bRank-System §8§m──────"));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§7Verwalte Gruppen, Prefixe und temporäre Ränge."));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8» §b/rank help §7– zeigt alle Befehle"));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    private Long parseDuration(String input) {
        if (input == null || input.isBlank()) return null;

        input = input.toLowerCase(Locale.ROOT).trim();

        long multiplier;
        if (input.endsWith("m")) { multiplier = 60_000L; input = input.replace("m", ""); }           // Minuten
        else if (input.endsWith("h")) { multiplier = 3_600_000L; input = input.replace("h", ""); }   // Stunden
        else if (input.endsWith("d")) { multiplier = 86_400_000L; input = input.replace("d", ""); }  // Tage
        else if (input.endsWith("w")) { multiplier = 604_800_000L; input = input.replace("w", ""); } // Wochen
        else if (input.endsWith("mo")) { multiplier = 2_592_000_000L; input = input.replace("mo", ""); } // Monate (30 Tage)
        else if (input.endsWith("y")) { multiplier = 31_536_000_000L; input = input.replace("y", ""); }  // Jahre
        else return null; // kein gültiges Format

        try {
            long num = Long.parseLong(input);
            return num * multiplier;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sendFullHelp(CommandSource src) {
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8§m──────§r §bGalacticfy §7| §bRank-Hilfe §8§m──────"));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§bAllgemein"));
        src.sendMessage(Component.text("§8» §b/rank help §7– diese Hilfe"));
        src.sendMessage(Component.text("§8» §b/rank group ... §7– Gruppen verwalten"));
        src.sendMessage(Component.text("§8» §b/rank user ... §7– Spieler-Ränge verwalten (auch temporär)"));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§bGruppen"));
        src.sendMessage(Component.text("§8» §b/rank group list"));
        src.sendMessage(Component.text("    §7Listet alle Gruppen."));
        src.sendMessage(Component.text("§8» §b/rank group create <name> <display> [prio]"));
        src.sendMessage(Component.text("    §7Neue Gruppe erstellen."));
        src.sendMessage(Component.text("§8» §b/rank group delete <name>"));
        src.sendMessage(Component.text("    §7Gruppe löschen."));
        src.sendMessage(Component.text("§8» §b/rank group info <name>"));
        src.sendMessage(Component.text("    §7Infos zu einer Gruppe (Prefix, Farbe, Inherit, etc.)."));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§bGruppen-Permissions"));
        src.sendMessage(Component.text("§8» §b/rank group permissions <name>"));
        src.sendMessage(Component.text("    §7Listet alle Permissions der Gruppe."));
        src.sendMessage(Component.text("§8» §b/rank group <name> set permission <node>"));
        src.sendMessage(Component.text("    §7Permission hinzufügen (z.B. §fgalacticfy.maintenance.simple§7)."));
        src.sendMessage(Component.text("§8» §b/rank group <name> unset permission <node>"));
        src.sendMessage(Component.text("    §7Permission entfernen."));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§bGruppen-Meta"));
        src.sendMessage(Component.text("§8» §b/rank group set prefix <Gruppe> <Prefix...>"));
        src.sendMessage(Component.text("    §7Setzt den Prefix der Gruppe."));
        src.sendMessage(Component.text("§8» §b/rank group set suffix <Gruppe> <Suffix...>"));
        src.sendMessage(Component.text("    §7Setzt den Suffix der Gruppe."));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§bGruppen-Inherit"));
        src.sendMessage(Component.text("§8» §b/rank group inherit list <Gruppe>"));
        src.sendMessage(Component.text("    §7Zeigt vererbte Eltern-Gruppen."));
        src.sendMessage(Component.text("§8» §b/rank group inherit add <Gruppe> <Parent>"));
        src.sendMessage(Component.text("    §7Gruppe erbt alle Rechte von Parent."));
        src.sendMessage(Component.text("§8» §b/rank group inherit remove <Gruppe> <Parent>"));
        src.sendMessage(Component.text("    §7Inheritance wieder entfernen."));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§bSpieler-Ränge"));
        src.sendMessage(Component.text("§8» §b/rank user <Spieler> set group <Gruppe> [Dauer]"));
        src.sendMessage(Component.text("    §7Setzt die Gruppe eines Spielers (z.B. §f7d, 3h, 30m, 1mo§7)."));
        src.sendMessage(Component.text("§8» §b/rank user <Spieler> unset group"));
        src.sendMessage(Component.text("    §7Setzt den Spieler auf die Default-Gruppe zurück."));
        src.sendMessage(Component.text(" "));

        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    private void sendGroupUsage(CommandSource src) {
        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§bGruppen-Verwaltung")));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text("§8» §b/rank group list"));
        src.sendMessage(Component.text("§8» §b/rank group create <name> <display> [prio]"));
        src.sendMessage(Component.text("§8» §b/rank group delete <name>"));
        src.sendMessage(Component.text("§8» §b/rank group info <name>"));
        src.sendMessage(Component.text("§8» §b/rank group permissions <name>"));
        src.sendMessage(Component.text("§8» §b/rank group <name> set permission <node>"));
        src.sendMessage(Component.text("§8» §b/rank group <name> unset permission <node>"));
        src.sendMessage(Component.text("§8» §b/rank group set prefix <Gruppe> <Prefix...>"));
        src.sendMessage(Component.text("§8» §b/rank group set suffix <Gruppe> <Suffix...>"));
        src.sendMessage(Component.text("§8» §b/rank group inherit list <Gruppe>"));
        src.sendMessage(Component.text("§8» §b/rank group inherit add <Gruppe> <Parent>"));
        src.sendMessage(Component.text("§8» §b/rank group inherit remove <Gruppe> <Parent>"));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    private void sendUserUsage(CommandSource src) {
        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§bUser-Gruppen-Verwaltung")));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text("§8» §b/rank user <Spieler> set group <Gruppe> [Dauer]"));
        src.sendMessage(Component.text("§8» §b/rank user <Spieler> unset group"));
        src.sendMessage(Component.text("§8§m────────────────────────────────"));
        src.sendMessage(Component.text(" "));
    }

    // ============================================================
    // PERMISSION GATE
    // ============================================================

    @Override
    public boolean hasPermission(Invocation invocation) {
        return isRankAdmin(invocation.source());
    }

    // ============================================================
    // TAB COMPLETION
    // ============================================================

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource src = invocation.source();

        if (!isRankAdmin(src)) {
            return List.of();
        }

        String[] args = invocation.arguments();

        // /rank <tab>
        if (args.length == 0) {
            return List.of("help", "group", "user");
        }

        if (args.length == 1) {
            String first = args[0].toLowerCase(Locale.ROOT);
            List<String> root = List.of("help", "group", "user");
            if (first.isEmpty()) return root;

            List<String> out = new ArrayList<>();
            for (String opt : root) {
                if (opt.startsWith(first)) out.add(opt);
            }
            return out;
        }

        String first = args[0].toLowerCase(Locale.ROOT);

        // ---------- group ----------
        if (first.equals("group")) {
            // /rank group <tab>
            if (args.length == 2) {
                String second = args[1].toLowerCase(Locale.ROOT);
                List<String> options = new ArrayList<>();
                options.add("list");
                options.add("create");
                options.add("delete");
                options.add("info");
                options.add("permissions");
                options.add("inherit");
                options.add("set");
                // zusätzlich Gruppennamen
                options.addAll(perms.getAllRoleNames());

                if (second.isEmpty()) return options;

                List<String> out = new ArrayList<>();
                for (String opt : options) {
                    if (opt.toLowerCase(Locale.ROOT).startsWith(second)) out.add(opt);
                }
                return out;
            }

            // /rank group delete|info|permissions <tab>
            if (args.length == 3) {
                String second = args[1].toLowerCase(Locale.ROOT);
                String thirdPrefix = args[2].toLowerCase(Locale.ROOT);

                if (second.equals("delete") || second.equals("info") || second.equals("permissions")) {
                    List<String> roles = perms.getAllRoleNames();
                    if (thirdPrefix.isEmpty()) return roles;
                    List<String> out = new ArrayList<>();
                    for (String r : roles) {
                        if (r.toLowerCase(Locale.ROOT).startsWith(thirdPrefix)) out.add(r);
                    }
                    return out;
                }

                if (second.equals("inherit")) {
                    List<String> opts = List.of("list", "add", "remove");
                    List<String> out = new ArrayList<>();
                    for (String o : opts) {
                        if (o.startsWith(thirdPrefix)) out.add(o);
                    }
                    return out;
                }

                if (second.equals("set")) {
                    List<String> opts = List.of("prefix", "suffix");
                    List<String> out = new ArrayList<>();
                    for (String o : opts) {
                        if (o.startsWith(thirdPrefix)) out.add(o);
                    }
                    return out;
                }

                // /rank group <name> <tab>
                List<String> subs = List.of("permissions", "set", "unset");
                List<String> out = new ArrayList<>();
                for (String s : subs) {
                    if (s.startsWith(thirdPrefix)) out.add(s);
                }
                return out;
            }

            // /rank group inherit <...> Tab-Complete
            if (args.length == 4 && args[1].equalsIgnoreCase("inherit")) {
                String mode = args[2].toLowerCase(Locale.ROOT);
                String pfx = args[3].toLowerCase(Locale.ROOT);
                if (mode.equals("list") || mode.equals("add") || mode.equals("remove")) {
                    List<String> roles = perms.getAllRoleNames();
                    if (pfx.isEmpty()) return roles;
                    List<String> out = new ArrayList<>();
                    for (String r : roles) {
                        if (r.toLowerCase(Locale.ROOT).startsWith(pfx)) out.add(r);
                    }
                    return out;
                }
            }

            if (args.length == 5 && args[1].equalsIgnoreCase("inherit")) {
                String mode = args[2].toLowerCase(Locale.ROOT);
                if (mode.equals("add") || mode.equals("remove")) {
                    String pfx = args[4].toLowerCase(Locale.ROOT);
                    List<String> roles = perms.getAllRoleNames();
                    if (pfx.isEmpty()) return roles;
                    List<String> out = new ArrayList<>();
                    for (String r : roles) {
                        if (r.toLowerCase(Locale.ROOT).startsWith(pfx)) out.add(r);
                    }
                    return out;
                }
            }

            // /rank group set prefix|suffix <tab> → Gruppen
            if (args[1].equalsIgnoreCase("set") && args.length == 4) {
                String type = args[2].toLowerCase(Locale.ROOT);
                if (type.equals("prefix") || type.equals("suffix")) {
                    String pfx = args[3].toLowerCase(Locale.ROOT);
                    List<String> roles = perms.getAllRoleNames();
                    if (pfx.isEmpty()) return roles;
                    List<String> out = new ArrayList<>();
                    for (String r : roles) {
                        if (r.toLowerCase(Locale.ROOT).startsWith(pfx)) out.add(r);
                    }
                    return out;
                }
            }

            return List.of();
        }

        // ---------- user ----------
        if (first.equals("user")) {
            // /rank user <tab> → Spielernamen
            if (args.length == 2) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                List<String> out = new ArrayList<>();
                for (Player p : proxy.getAllPlayers()) {
                    String n = p.getUsername();
                    if (prefix.isEmpty() || n.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        out.add(n);
                    }
                }
                return out;
            }

            // /rank user <spieler> <tab> → set / unset
            if (args.length == 3) {
                String third = args[2].toLowerCase(Locale.ROOT);
                List<String> opts = List.of("set", "unset");
                List<String> out = new ArrayList<>();
                for (String o : opts) {
                    if (o.startsWith(third)) out.add(o);
                }
                return out;
            }

            String action = args[2].toLowerCase(Locale.ROOT);

            if (action.equals("set")) {
                // /rank user <spieler> set <tab> → group
                if (args.length == 4) {
                    String pfx = args[3].toLowerCase(Locale.ROOT);
                    if ("group".startsWith(pfx)) return List.of("group");
                    return List.of();
                }
                // /rank user <spieler> set group <tab> → Gruppen
                if (args.length == 5 && "group".equalsIgnoreCase(args[3])) {
                    String pfx = args[4].toLowerCase(Locale.ROOT);
                    List<String> roles = perms.getAllRoleNames();
                    if (pfx.isEmpty()) return roles;
                    List<String> out = new ArrayList<>();
                    for (String r : roles) {
                        if (r.toLowerCase(Locale.ROOT).startsWith(pfx)) out.add(r);
                    }
                    return out;
                }

                // 🆕 /rank user <spieler> set group <gruppe> <tab> → Zeitvorschläge
                if (args.length == 6 && "group".equalsIgnoreCase(args[3])) {
                    String pfx = args[5].toLowerCase(Locale.ROOT);
                    List<String> durations = List.of("30m", "1h", "3h", "12h", "1d", "7d", "1w", "1mo", "1y");

                    if (pfx.isEmpty()) return durations;

                    List<String> out = new ArrayList<>();
                    for (String d : durations) {
                        if (d.toLowerCase(Locale.ROOT).startsWith(pfx)) {
                            out.add(d);
                        }
                    }
                    return out;
                }
            }

            if (action.equals("unset")) {
                // /rank user <spieler> unset <tab> → group
                if (args.length == 4) {
                    String pfx = args[3].toLowerCase(Locale.ROOT);
                    if ("group".startsWith(pfx)) return List.of("group");
                    return List.of();
                }
            }
        }

        return List.of();
    }
}
