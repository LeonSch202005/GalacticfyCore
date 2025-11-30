package de.galacticfy.core.service;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.galacticfy.core.service.QuestService.PlayerQuestView;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class QuestGuiMessenger {

    private final ProxyServer proxy;
    private final ChannelIdentifier channel;
    private final Logger logger;
    private final QuestService questService;

    public QuestGuiMessenger(ProxyServer proxy,
                             ChannelIdentifier channel,
                             Logger logger,
                             QuestService questService) {
        this.proxy = proxy;
        this.channel = channel;
        this.logger = logger;
        this.questService = questService;
    }

    // beim /quests-Command aufgerufen
    public void openGui(Player player, List<PlayerQuestView> quests) {
        byte[] payload = serialize(quests);
        player.getCurrentServer().ifPresent(conn -> {
            conn.sendPluginMessage(channel, payload);
        });
    }

    // wird vom QuestService über den updateHook benutzt
    public void pushUpdate(UUID uuid) {
        Player player = proxy.getPlayer(uuid).orElse(null);
        if (player == null) {
            return;
        }
        List<PlayerQuestView> list = questService.getQuestsFor(uuid);
        byte[] payload = serialize(list);
        player.getCurrentServer().ifPresent(conn -> {
            conn.sendPluginMessage(channel, payload);
        });
    }

    private byte[] serialize(List<PlayerQuestView> quests) {
        StringBuilder sb = new StringBuilder();
        for (PlayerQuestView view : quests) {
            var def = view.definition();
            long progress = view.progress();
            boolean completed = view.completed();

            sb.append(def.key()).append('|')
                    .append(def.title()).append('|')
                    .append(def.description().replace('\n', ' ')).append('|')
                    .append(def.type().name()).append('|')
                    .append(def.goal()).append('|')
                    .append(progress).append('|')
                    .append(def.rewardGalas()).append('|')
                    .append(def.rewardStardust()).append('|')
                    .append(completed ? '1' : '0')
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
