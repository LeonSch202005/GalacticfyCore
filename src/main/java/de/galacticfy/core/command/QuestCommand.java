package de.galacticfy.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.galacticfy.core.service.QuestGuiMessenger;
import de.galacticfy.core.service.QuestService;
import de.galacticfy.core.service.QuestService.PlayerQuestView;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class QuestCommand implements SimpleCommand {

    private static final String PERM_ADMIN = "galacticfy.quests.admin";

    private final QuestService quests;
    private final QuestGuiMessenger guiMessenger;

    public QuestCommand(QuestService quests, QuestGuiMessenger guiMessenger) {
        this.quests = quests;
        this.guiMessenger = guiMessenger;
    }

    private Component prefix() {
        return Component.text("§8[§dQuests§8] §r");
    }

    @Override
    public void execute(Invocation invocation) {
        var src  = invocation.source();
        var args = invocation.arguments();

        // /quests → GUI öffnen
        if (args.length == 0) {
            if (!(src instanceof Player p)) {
                src.sendMessage(prefix().append(Component.text("§cNur Spieler können Quests ansehen.")));
                return;
            }

            List<PlayerQuestView> list = quests.getQuestsFor(p.getUniqueId());
            guiMessenger.openGui(p, list);

            p.sendMessage(prefix().append(Component.text("§7Deine Quests wurden im GUI geöffnet.")));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean admin = hasAdmin(invocation);

        if (sub.equals("reload")) {
            if (!admin) {
                src.sendMessage(prefix().append(Component.text("§cDazu hast du keine Berechtigung.")));
                return;
            }
            quests.reloadDefinitions();
            src.sendMessage(prefix().append(Component.text("§aQuests wurden neu geladen.")));
            return;
        }

        if (sub.equals("info") && args.length >= 2) {
            String key = args[1].toLowerCase(Locale.ROOT);
            var def = quests.getDefinition(key);
            if (def == null) {
                src.sendMessage(prefix().append(Component.text("§cDiese Quest existiert nicht: §f" + key)));
                return;
            }

            src.sendMessage(Component.text(" "));
            src.sendMessage(prefix().append(Component.text("§dQuest-Info: §f" + def.title())));
            src.sendMessage(Component.text(" "));
            src.sendMessage(Component.text("§7Key: §f" + def.key()));
            src.sendMessage(Component.text("§7Typ: §f" + def.type().name()));
            src.sendMessage(Component.text("§7Beschreibung: §f" + def.description()));
            src.sendMessage(Component.text("§7Ziel: §e" + def.goal()));
            src.sendMessage(Component.text("§7Belohnung: §e" + def.rewardGalas() + "⛃ §7/ §d" + def.rewardStardust() + "✧"));
            src.sendMessage(Component.text("§7Aktiv: " + (def.active() ? "§aJa" : "§cNein")));
            src.sendMessage(Component.text(" "));
            return;
        }

        sendUsage(src, admin);
    }

    private boolean hasAdmin(Invocation invocation) {
        var src = invocation.source();
        if (!(src instanceof Player p)) {
            return true; // Konsole darf alles
        }
        return p.hasPermission(PERM_ADMIN);
    }

    private void sendUsage(com.velocitypowered.api.command.CommandSource src, boolean admin) {
        src.sendMessage(Component.text(" "));
        src.sendMessage(prefix().append(Component.text("§dQuests §7| §dÜbersicht")));
        src.sendMessage(Component.text(" "));
        src.sendMessage(Component.text("§8» §d/quests §7- Öffnet deine Quest-GUI."));
        src.sendMessage(Component.text("§8» §d/quests info <key> §7- Details zu einer Quest."));
        if (admin) {
            src.sendMessage(Component.text("§8» §d/quests reload §7- Quests neu laden."));
        }
        src.sendMessage(Component.text(" "));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        var args = invocation.arguments();
        boolean admin = hasAdmin(invocation);

        if (args.length == 0) {
            return admin ? List.of("info", "reload") : List.of("info");
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("info", "reload").stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return quests.getActiveQuestKeys().stream()
                    .filter(k -> k.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String::compareToIgnoreCase)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // /quests darf jeder benutzen
        return true;
    }
}
