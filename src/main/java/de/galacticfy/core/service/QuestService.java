package de.galacticfy.core.service;

import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class QuestService {

    public enum QuestType {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    public record QuestDefinition(
            String key,
            String title,
            String description,
            QuestType type,
            long goal,
            long rewardGalas,
            long rewardStardust,
            boolean active
    ) {}

    public record PlayerQuestView(
            QuestDefinition definition,
            long progress,
            boolean completed
    ) {}

    private static final class QuestProgress {
        long value;
        boolean completedForPeriod;
        long lastPeriodId; // DAILY: epochDay, WEEKLY: epochDay/7, MONTHLY: year*12+month
    }

    private final Logger logger;
    private final EconomyService economy;

    private final Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
    private final Map<UUID, Map<String, QuestProgress>> progressMap = new ConcurrentHashMap<>();

    // wird von außen gesetzt (QuestGuiMessenger), um Live-Updates zu pushen
    private Consumer<UUID> updateHook;

    public QuestService(Logger logger, EconomyService economy) {
        this.logger = logger;
        this.economy = economy;
        registerDefaultDefinitions();
    }

    public void setUpdateHook(Consumer<UUID> hook) {
        this.updateHook = hook;
    }

    // =====================================================
    // Definitionen
    // =====================================================

    private void registerDefaultDefinitions() {
        definitions.clear();

        // ---------- DAILY ----------
        register(new QuestDefinition(
                "daily_login",
                "Täglicher Login",
                "Logge dich einmal am Tag ein.",
                QuestType.DAILY,
                1,
                250,
                0,
                true
        ));

        register(new QuestDefinition(
                "daily_play_30",
                "30 Minuten zocken",
                "Spiele insgesamt 30 Minuten auf dem Netzwerk.",
                QuestType.DAILY,
                30,
                500,
                1,
                true
        ));

        register(new QuestDefinition(
                "daily_play_60",
                "1 Stunde zocken",
                "Spiele insgesamt 60 Minuten auf dem Netzwerk.",
                QuestType.DAILY,
                60,
                750,
                1,
                true
        ));

        register(new QuestDefinition(
                "daily_play_120",
                "2 Stunden Grind",
                "Spiele insgesamt 120 Minuten auf dem Netzwerk.",
                QuestType.DAILY,
                120,
                1250,
                2,
                true
        ));

        register(new QuestDefinition(
                "daily_earn_1000_galas",
                "Täglicher Verdiener",
                "Verdiene heute insgesamt 1.000 Galas.",
                QuestType.DAILY,
                1_000,
                0,
                1,
                true
        ));

        register(new QuestDefinition(
                "daily_break_200_blocks",
                "Blockschredder",
                "Baue heute 200 Blöcke ab.",
                QuestType.DAILY,
                200,
                400,
                0,
                true
        ));

        register(new QuestDefinition(
                "daily_place_150_blocks",
                "Baumeister",
                "Platziere heute 150 Blöcke.",
                QuestType.DAILY,
                150,
                400,
                0,
                true
        ));

        register(new QuestDefinition(
                "daily_kill_30_mobs",
                "Mob-Jäger",
                "Besiege heute 30 Mobs.",
                QuestType.DAILY,
                30,
                600,
                1,
                true
        ));

        // ---------- WEEKLY ----------
        register(new QuestDefinition(
                "weekly_play_300",
                "Stammspieler",
                "Spiele 300 Minuten in dieser Woche.",
                QuestType.WEEKLY,
                300,
                2000,
                3,
                true
        ));

        register(new QuestDefinition(
                "weekly_play_600",
                "Hardcore Grinder",
                "Spiele 600 Minuten in dieser Woche.",
                QuestType.WEEKLY,
                600,
                3500,
                5,
                true
        ));

        register(new QuestDefinition(
                "weekly_earn_5000_galas",
                "Wöchentlicher Verdiener",
                "Verdiene 5.000 Galas in dieser Woche.",
                QuestType.WEEKLY,
                5_000,
                0,
                5,
                true
        ));

        register(new QuestDefinition(
                "weekly_break_1000_blocks",
                "Abbruchunternehmen",
                "Baue 1.000 Blöcke in dieser Woche ab.",
                QuestType.WEEKLY,
                1_000,
                2500,
                2,
                true
        ));

        register(new QuestDefinition(
                "weekly_kill_200_mobs",
                "Mob-Schlächter",
                "Besiege 200 Mobs in dieser Woche.",
                QuestType.WEEKLY,
                200,
                3000,
                3,
                true
        ));

        // ---------- MONTHLY ----------
        register(new QuestDefinition(
                "monthly_play_2000",
                "Monatslegende",
                "Spiele 2.000 Minuten in diesem Monat.",
                QuestType.MONTHLY,
                2_000,
                10_000,
                10,
                true
        ));

        register(new QuestDefinition(
                "monthly_earn_50000_galas",
                "Galactic Händler",
                "Verdiene 50.000 Galas in diesem Monat.",
                QuestType.MONTHLY,
                50_000,
                0,
                20,
                true
        ));

        register(new QuestDefinition(
                "monthly_break_5000_blocks",
                "Weltformer",
                "Baue 5.000 Blöcke in diesem Monat ab.",
                QuestType.MONTHLY,
                5_000,
                7000,
                5,
                true
        ));

        register(new QuestDefinition(
                "monthly_kill_500_mobs",
                "Monstervernichter",
                "Besiege 500 Mobs in diesem Monat.",
                QuestType.MONTHLY,
                500,
                7000,
                5,
                true
        ));

        logger.info("QuestService: {} Quest-Definitionen geladen.", definitions.size());
    }

    private void register(QuestDefinition def) {
        definitions.put(def.key().toLowerCase(Locale.ROOT), def);
    }

    public void reloadDefinitions() {
        logger.info("QuestService: Quest-Definitionen werden neu geladen...");
        registerDefaultDefinitions();
        logger.info("QuestService: Reload abgeschlossen (Fortschritt nicht zurückgesetzt).");
    }

    public QuestDefinition getDefinition(String key) {
        if (key == null) return null;
        return definitions.get(key.toLowerCase(Locale.ROOT));
    }

    public List<String> getActiveQuestKeys() {
        return definitions.values().stream()
                .filter(QuestDefinition::active)
                .map(QuestDefinition::key)
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    // ============================
    // Spieler-Ansicht
    // ============================

    public List<PlayerQuestView> getQuestsFor(UUID uuid) {
        if (uuid == null) return List.of();

        Map<String, QuestProgress> map = progressMap.get(uuid);
        List<PlayerQuestView> result = new ArrayList<>();

        for (QuestDefinition def : definitions.values()) {
            if (!def.active()) continue;

            QuestProgress qp = (map != null) ? map.get(def.key()) : null;
            long value = (qp != null) ? qp.value : 0L;
            boolean completed = (qp != null) && qp.completedForPeriod;

            result.add(new PlayerQuestView(def, value, completed));
        }

        return result;
    }

    // ============================
    // Hooks für andere Module
    // ============================

    public void handleLogin(UUID uuid, String name,
                            BiConsumer<UUID, Component> sender) {
        if (uuid == null) return;
        increment(uuid, name, "daily_login", 1, sender);
    }

    public void handlePlaytime(UUID uuid, String name, long minutes,
                               BiConsumer<UUID, Component> sender) {
        if (uuid == null || minutes <= 0) return;

        increment(uuid, name, "daily_play_30", minutes, sender);
        increment(uuid, name, "daily_play_60", minutes, sender);
        increment(uuid, name, "daily_play_120", minutes, sender);

        increment(uuid, name, "weekly_play_300", minutes, sender);
        increment(uuid, name, "weekly_play_600", minutes, sender);

        increment(uuid, name, "monthly_play_2000", minutes, sender);
    }

    public void handleEarnedGalas(UUID uuid, String name, long amount,
                                  BiConsumer<UUID, Component> sender) {
        if (uuid == null || amount <= 0) return;

        increment(uuid, name, "daily_earn_1000_galas", amount, sender);
        increment(uuid, name, "weekly_earn_5000_galas", amount, sender);
        increment(uuid, name, "monthly_earn_50000_galas", amount, sender);
    }

    public void handleBlocksBroken(UUID uuid, String name, long amount,
                                   BiConsumer<UUID, Component> sender) {
        if (uuid == null || amount <= 0) return;

        increment(uuid, name, "daily_break_200_blocks", amount, sender);
        increment(uuid, name, "weekly_break_1000_blocks", amount, sender);
        increment(uuid, name, "monthly_break_5000_blocks", amount, sender);
    }

    public void handleMobsKilled(UUID uuid, String name, long amount,
                                 BiConsumer<UUID, Component> sender) {
        if (uuid == null || amount <= 0) return;

        increment(uuid, name, "daily_kill_30_mobs", amount, sender);
        increment(uuid, name, "weekly_kill_200_mobs", amount, sender);
        increment(uuid, name, "monthly_kill_500_mobs", amount, sender);
    }

    // ============================
    // Intern: Progress + Rewards
    // ============================

    private void increment(UUID uuid,
                           String name,
                           String questKey,
                           long delta,
                           BiConsumer<UUID, Component> sender) {

        QuestDefinition def = getDefinition(questKey);
        if (def == null || !def.active()) return;

        long today = LocalDate.now().toEpochDay();
        long periodId = switch (def.type()) {
            case DAILY   -> today;
            case WEEKLY  -> today / 7;
            case MONTHLY -> {
                var d = LocalDate.now();
                yield d.getYear() * 12L + d.getMonthValue();
            }
        };

        Map<String, QuestProgress> playerMap =
                progressMap.computeIfAbsent(uuid, id -> new ConcurrentHashMap<>());

        QuestProgress qp = playerMap.computeIfAbsent(def.key(), k -> new QuestProgress());

        if (qp.lastPeriodId != periodId) {
            qp.value = 0;
            qp.completedForPeriod = false;
        }
        qp.lastPeriodId = periodId;

        if (qp.completedForPeriod) {
            qp.value = Math.min(qp.value + delta, def.goal());
            return;
        }

        qp.value += delta;

        // nach jedem Fortschritts-Update Live-Update auslösen
        if (updateHook != null) {
            updateHook.accept(uuid);
        }

        if (qp.value >= def.goal()) {
            qp.completedForPeriod = true;
            qp.value = def.goal();

            if (def.rewardGalas() > 0) {
                economy.deposit(uuid, def.rewardGalas());
            }
            if (def.rewardStardust() > 0) {
                economy.addStardust(uuid, def.rewardStardust());
            }

            if (sender != null) {
                sender.accept(uuid, Component.text(
                        "§d[Quests] §7Quest §d" + def.title() +
                                " §7abgeschlossen! Belohnung: " +
                                (def.rewardGalas() > 0 ? "§e" + def.rewardGalas() + "⛃ " : "") +
                                (def.rewardStardust() > 0 ? "§d" + def.rewardStardust() + "✧" : "")
                ));
            }

            logger.info("QuestService: Quest '{}' von {} ({}) abgeschlossen.",
                    def.key(), name, uuid);
        }
    }
}
