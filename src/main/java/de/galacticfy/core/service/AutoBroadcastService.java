package de.galacticfy.core.service;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AutoBroadcastService {

    private final ProxyServer proxy;
    private final MessageService messageService;
    private final Logger logger;
    private final Object pluginInstance;

    private ScheduledTask task;

    // Produktivwerte
    private static final int MIN_PLAYERS_FOR_BROADCAST = 5;
    private static final Duration FIRST_DELAY  = Duration.ofMinutes(10);
    private static final Duration INTERVAL     = Duration.ofMinutes(15);

    private final List<String> messages = new ArrayList<>();
    private final Random random = new Random();
    private List<String> shuffledPool = new ArrayList<>();
    private int currentIndex = 0;

    public AutoBroadcastService(ProxyServer proxy,
                                MessageService messageService,
                                Logger logger,
                                Object pluginInstance) {
        this.proxy = proxy;
        this.messageService = messageService;
        this.logger = logger;
        this.pluginInstance = pluginInstance;

        loadDefaultMessages();
    }

    private void loadDefaultMessages() {
        messages.add("Trete unserem Discord bei: §b/discord");
        messages.add("Du brauchst Hilfe? Nutze §b/report <Spieler> <Grund>");
        messages.add("Besuche unsere Website: §bhttps://galacticfy.de");
        messages.add("Halte dich an die Regeln, um Strafen zu vermeiden.");
        messages.add("Du findest einen Bug? Melde ihn im Support oder per §b/discord§7.");
        messages.add("Du benötigst Unterstützung? Unser Team hilft dir gerne weiter!");
        messages.add("Achtung: Respektvoller Umgang ist Pflicht – sei freundlich zu anderen Spielern.");
        messages.add("Nutze §b/hub§7, um schnell zur Lobby zurückzukehren.");
        messages.add("Lade Freunde ein und spielt gemeinsam auf Galacticfy!");
        messages.add("Schütze deinen Account! Nutze niemals Passwörter mehrfach auf anderen Servern.");
        messages.add("Teammitglieder fragen dich §cniemals §7nach deinem Passwort!");
        messages.add("Achte darauf, keine verdächtigen Mods herunterzuladen – Sicherheit geht vor!");
        messages.add("Hast du Ideen oder Feedback? Teile es mit uns auf Discord!");
        messages.add("Bereit für neue Abenteuer? Probiere unsere Spielmodi aus!");
        messages.add("Events finden regelmäßig statt – halte Ausschau nach Ankündigungen.");
        messages.add("Bleibe fair im Spiel – Cheating führt zu dauerhaften Strafen.");
        messages.add("Unterstütze das Netzwerk: Teile den Server mit deinen Freunden!");
        messages.add("Mehr Features, mehr Spaß – wir arbeiten ständig an Verbesserungen!");
        messages.add("Bleibe am Ball! Bald erscheinen neue Inhalte und Features.");
    }

    public void start() {
        if (task != null) return;

        if (messages.isEmpty()) {
            logger.warn("AutoBroadcastService gestartet, aber keine Nachrichten definiert.");
            return;
        }

        reshufflePool();

        this.task = proxy.getScheduler()
                .buildTask(pluginInstance, this::sendNext)
                .delay(FIRST_DELAY)
                .repeat(INTERVAL)
                .schedule();

        logger.info(
                "AutoBroadcastService gestartet ({} Nachrichten, Intervall {} Minuten, Mindestspieler: {}).",
                messages.size(),
                INTERVAL.toMinutes(),
                MIN_PLAYERS_FOR_BROADCAST
        );
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
            logger.info("AutoBroadcastService wurde gestoppt.");
        }
    }

    private void reshufflePool() {
        shuffledPool = new ArrayList<>(messages);
        Collections.shuffle(shuffledPool, random);
        currentIndex = 0;
    }

    private void sendNext() {
        int online = proxy.getPlayerCount();

        if (online < MIN_PLAYERS_FOR_BROADCAST) {
            logger.debug("AutoBroadcast übersprungen ({} Spieler online, Minimum {}).",
                    online, MIN_PLAYERS_FOR_BROADCAST);
            return;
        }

        if (shuffledPool.isEmpty() || currentIndex >= shuffledPool.size()) {
            reshufflePool();
        }

        String msg = shuffledPool.get(currentIndex++);
        messageService.announce("§7" + msg);
    }

    public void addMessage(String message) {
        if (message == null || message.isBlank()) return;
        messages.add(message);
        reshufflePool();
    }

    public List<String> getMessages() {
        return List.copyOf(messages);
    }
}
