package de.galacticfy.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.galacticfy.core.service.QuestGuiMessenger;
import de.galacticfy.core.service.QuestService;
import net.kyori.adventure.text.Component;

import java.util.*;

public class QuestCommand implements SimpleCommand {

    private static final String PERM_ADMIN = "galacticfy.quests.admin";

    private final QuestService quests;
    private final QuestGuiMessenger guiMessenger;
    private final ProxyServer proxy;

    public QuestCommand(QuestService quests, QuestGuiMessenger guiMessenger, ProxyServer proxy) {
        this.quests = quests;
        this.guiMessenger = guiMessenger;
        this.proxy = proxy;
    }

    private Component prefix() {
        return Component.text("§8[§dQuests§8] §r");
    }

    private String sourceName(CommandSource source) {
        if (source instanceof Player p) {
            return p.getUsername();
        }
        return "Konsole";
    }

    private boolean isAdmin(CommandSource src) {
        return src.hasPermission(PERM_ADMIN);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        // /quests -> GUI öffnen
        if (args.length == 0) {
            if (src instanceof Player player) {
                guiMessenger.openGui(player);
            } else {
                src.sendMessage(prefix().append(Component.text("§cNur Ingame-Spieler können das Quest-Menü öffnen.")));
            }
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "help":
            case "?": {
                sendHelp(src);
                return;
            }

            case "reload": {
                if (!isAdmin(src)) {
                    src.sendMessage(prefix().append(Component.text("§cDafür hast du keine Rechte.")));
                    return;
                }

                quests.reloadDefinitions();
                src.sendMessage(prefix().append(Component.text("§aQuest-Definitionen wurden neu generiert.")));
                return;
            }

            case "reroll": {
                if (!isAdmin(src)) {
                    src.sendMessage(prefix().append(Component.text("§cDafür hast du keine Rechte.")));
                    return;
                }

                quests.forceRandomRoll();
                src.sendMessage(prefix().append(Component.text("§aForce-Roll ausgeführt, heutige Quests wurden neu ausgewürfelt.")));
                return;
            }

            case "reset": {
                if (!isAdmin(src)) {
                    src.sendMessage(prefix().append(Component.text("§cDafür hast du keine Rechte.")));
                    return;
                }

                if (args.length < 2) {
                    src.sendMessage(prefix().append(Component.text("§7Benutzung: §e/quests reset <all|Spieler>")));
                    return;
                }

                String target = args[1];

                // /quests reset all
                if (target.equalsIgnoreCase("all") || target.equalsIgnoreCase("*")) {
                    quests.resetAllProgress();
                    src.sendMessage(prefix().append(Component.text("§aAlle Quest-Fortschritte wurden zurückgesetzt.")));
                    return;
                }

                // /quests reset <Spieler> (nur Online-Spieler)
                Optional<Player> opt = proxy.getPlayer(target);
                if (opt.isEmpty()) {
                    src.sendMessage(prefix().append(Component.text("§cSpieler §e" + target + " §cwurde nicht gefunden (muss online sein).")));
                    return;
                }

                Player player = opt.get();
                quests.resetProgress(player.getUniqueId());
                src.sendMessage(prefix().append(Component.text("§aQuest-Fortschritt von §e" + player.getUsername() + " §awurde zurückgesetzt.")));
                player.sendMessage(prefix().append(Component.text("§cDein Quest-Fortschritt wurde von einem Admin zurückgesetzt.")));
                return;
            }

            default: {
                src.sendMessage(prefix().append(Component.text("§cUnbekannter Unterbefehl. §7Nutze §e/quests help§7.")));
            }
        }
    }

    private void sendHelp(CommandSource src) {
        src.sendMessage(prefix().append(Component.text("§d/quests §7- Öffnet das Quest-Menü (nur Spieler)")));
        if (isAdmin(src)) {
            src.sendMessage(prefix().append(Component.text("§d/quests reload §7- Daily/Weekly/Monthly/Event neu generieren")));
            src.sendMessage(prefix().append(Component.text("§d/quests reroll §7- Heutige Quests neu auswürfeln")));
            src.sendMessage(prefix().append(Component.text("§d/quests reset all §7- Alle Quest-Fortschritte löschen")));
            src.sendMessage(prefix().append(Component.text("§d/quests reset <Spieler> §7- Fortschritt eines Spielers löschen (online)")));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource src = invocation.source();

        if (args.length == 0) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            base.add("help");
            if (isAdmin(src)) {
                base.add("reload");
                base.add("reroll");
                base.add("reset");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String s : base) {
                if (s.startsWith(prefix)) result.add(s);
            }
            return result;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reset") && isAdmin(src)) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            if ("all".startsWith(prefix)) {
                result.add("all");
            }
            for (Player p : proxy.getAllPlayers()) {
                if (p.getUsername().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(p.getUsername());
                }
            }
            return result;
        }

        return List.of();
    }
}
