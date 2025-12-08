package de.galacticfy.core.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.galacticfy.core.service.QuestService;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Empfängt Stat-Updates vom Spigot-Plugin (galacticfy:queststats)
 * und routet sie in den QuestService.
 *
 * Format:
 *   TYPE|UUID|NAME|AMOUNT
 */
public class QuestEventListener {

    private final QuestService questService;
    private final ChannelIdentifier statsChannel;
    private final ProxyServer proxy;
    private final Logger logger;

    public QuestEventListener(QuestService questService,
                              ChannelIdentifier statsChannel,
                              ProxyServer proxy,
                              Logger logger) {
        this.questService = questService;
        this.statsChannel = statsChannel;
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getIdentifier().equals(statsChannel)) return;

        String msg = new String(e.getData(), StandardCharsets.UTF_8);

        String[] parts = msg.split("\\|");
        if (parts.length < 4) {
            logger.warn("[QuestStats] Ungültige Nachricht: {}", msg);
            return;
        }

        String type = parts[0];
        UUID uuid;
        try {
            uuid = UUID.fromString(parts[1]);
        } catch (Exception ex) {
            logger.warn("[QuestStats] Ungültige UUID: {}", parts[1]);
            return;
        }

        String name = parts[2];
        long amount = 1;

        try {
            amount = Long.parseLong(parts[3]);
        } catch (Exception ignored) {}

        // Routing
        routeStat(type, uuid, name, amount);
    }

    private void routeStat(String type, UUID uuid, String name, long amount) {
        switch (type.toUpperCase()) {

            // ========== MINING ==========
            case "STONE" -> questService.handleStoneBroken(uuid, name, amount, this::sendBar);
            case "ORE" -> questService.handleOresBroken(uuid, name, amount, this::sendBar);
            case "DIRT" -> questService.handleDirtBroken(uuid, name, amount, this::sendBar);
            case "GRAVEL" -> questService.handleGravelBroken(uuid, name, amount, this::sendBar);
            case "NETHERRACK" -> questService.handleNetherrackBroken(uuid, name, amount, this::sendBar);
            case "SAND_BREAK" -> questService.handleSandBroken(uuid, name, amount, this::sendBar);
            case "SAND_GIVE" -> questService.handleSandDelivered(uuid, name, amount, this::sendBar);
            case "WOOD" -> questService.handleWoodChopped(uuid, name, amount, this::sendBar);
            case "CROPS" -> questService.handleCropsHarvested(uuid, name, amount, this::sendBar);
            case "SUGARCANE" -> questService.handleSugarCaneHarvested(uuid, name, amount, this::sendBar);

            // ========== WALKING ==========
            case "WALK" -> questService.handleBlocksWalked(uuid, name, amount, this::sendBar);

            // ========== SMELTING ==========
            case "SMELT_ORES" -> questService.handleOresSmelted(uuid, name, amount, this::sendBar);
            case "SMELT_FOOD" -> questService.handleFoodSmelted(uuid, name, amount, this::sendBar);

            // ========== CRAFTING ==========
            case "CRAFT_TOOLS" -> questService.handleToolsCrafted(uuid, name, amount, this::sendBar);
            case "CRAFT_TORCHES" -> questService.handleTorchesCrafted(uuid, name, amount, this::sendBar);
            case "CRAFT_BREAD" -> questService.handleBreadCrafted(uuid, name, amount, this::sendBar);
            case "CRAFT_BLOCKS" -> questService.handleBlocksCrafted(uuid, name, amount, this::sendBar);
            case "CRAFT_GEAR" -> questService.handleGearCrafted(uuid, name, amount, this::sendBar);

            // ========== FISHING ==========
            case "FISH" -> questService.handleFishCaught(uuid, name, amount, this::sendBar);

            // ========== COMBAT ==========
            case "MOB" -> questService.handleMobsKilled(uuid, name, amount, this::sendBar);
            case "ZOMBIE" -> questService.handleZombiesKilled(uuid, name, amount, this::sendBar);
            case "CREEPER" -> questService.handleCreepersKilled(uuid, name, amount, this::sendBar);

            // ========== TRADES ==========
            case "TRADE" -> questService.handleTradesMade(uuid, name, amount, this::sendBar);

            // ========== PLAYTIME / LOGIN ==========
            case "PLAYTIME" -> questService.handlePlaytime(uuid, name, amount, this::sendBar);
            case "LOGIN" -> questService.handleLogin(uuid, name, this::sendBar);

            default -> logger.warn("[QuestStats] Unbekannter Stat: {}", type);
        }
    }

    private void sendBar(UUID uuid, Component msg) {
        proxy.getPlayer(uuid).ifPresent(p -> p.sendActionBar(msg));
    }
}
