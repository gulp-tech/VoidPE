package voidpe.chat;

import org.powernukkitx.Player;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.EventPriority;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerChatEvent;
import org.powernukkitx.event.player.PlayerJoinEvent;
import org.powernukkitx.event.player.PlayerQuitEvent;
import org.powernukkitx.event.player.PlayerRespawnEvent;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * VoidChat - local chat inside a radius + global chat with a leading "!".
 * Also puts the [VoidPE] nametag above the head (replaces PlaceholderChat).
 *
 * Local  : only players within "radius" blocks (same world) + console
 * Global : starts with "!" - everybody, the "!" is cut off
 *
 * NOTE: placeholders in texts are {player}, {message}, {seconds}.
 * Do NOT use {%0}/{%1} - PowerNukkitX eats any "%<word>" in the format
 * (BaseLang.parseLanguageText), so those tokens never reach the chat.
 *
 * Everything is event based: no repeating tasks.
 */
public class VoidChat extends PluginBase implements Listener {

    private final Map<String, Long> cooldowns = new HashMap<>();

    private double radius;
    private String globalSymbol;
    private String formatLocal;
    private String formatGlobal;
    private long cooldownMillis;
    private String cooldownMessage;
    private boolean nametagEnabled;
    private String nametag;

    @Override
    public void onEnable() {
        Config cfg = this.getConfig();

        this.radius = cfg.getInt("radius", 100);
        this.globalSymbol = cfg.getString("global-symbol", "!");
        this.formatLocal = cfg.getString("format-local",
                "\u00a7f{player}\u00a78 \u00bb \u00a77{message}");
        this.formatGlobal = cfg.getString("format-global",
                "\u00a78[\u00a76G\u00a78] \u00a7f{player}\u00a78 \u00bb \u00a77{message}");
        this.cooldownMillis = (long) (cfg.getDouble("cooldown-seconds", 2.0) * 1000.0);
        this.cooldownMessage = cfg.getString("cooldown-message",
                "\u00a77\u041d\u0435 \u0442\u0430\u043a \u0431\u044b\u0441\u0442\u0440\u043e: \u043f\u043e\u0434\u043e\u0436\u0434\u0438 \u0435\u0449\u0451 \u00a7f{seconds}\u00a77 \u0441\u0435\u043a.");
        this.nametagEnabled = cfg.getBoolean("nametag-enabled", true);
        this.nametag = cfg.getString("nametag", "\u00a78[\u00a76VoidPE\u00a78] \u00a7f{player}");

        this.getServer().getPluginManager().registerEvents(this, this);
        this.getLogger().info("VoidChat enabled. radius=" + (int) this.radius
                + ", global=\"" + this.globalSymbol + "\""
                + ", cooldown=" + (this.cooldownMillis / 1000.0) + "s"
                + ", nametag=" + this.nametagEnabled);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (this.cooldownMillis > 0 && !player.isOp()) {
            Long last = this.cooldowns.get(player.getName());
            if (last != null) {
                long left = this.cooldownMillis - (now - last);
                if (left > 0) {
                    event.setCancelled(true);
                    String seconds = String.valueOf((left + 999) / 1000);
                    player.sendMessage(this.cooldownMessage.replace("{seconds}", seconds));
                    return;
                }
            }
        }
        this.cooldowns.put(player.getName(), now);

        boolean global = !this.globalSymbol.isEmpty() && message.startsWith(this.globalSymbol);
        if (global) {
            message = message.substring(this.globalSymbol.length()).trim();
            if (message.isEmpty()) {
                event.setCancelled(true);
                return;
            }
        }

        event.setMessage(message);
        event.setFormat((global ? this.formatGlobal : this.formatLocal)
                .replace("{player}", player.getDisplayName())
                .replace("{message}", message));

        if (global) {
            return; // default recipients = everyone
        }

        Set<CommandSender> recipients = new HashSet<>();
        for (Player other : player.getLevel().getPlayers().values()) {
            if (other == player || player.distance(other) <= this.radius) {
                recipients.add(other);
            }
        }
        recipients.add(this.getServer().getConsoleSender());
        event.setRecipients(recipients);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyNametag(player);
        // one more time a second later, in case the client resets the tag after spawn
        this.getServer().getScheduler().scheduleDelayedTask(this, () -> applyNametag(player), 20);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        applyNametag(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.cooldowns.remove(event.getPlayer().getName());
    }

    private void applyNametag(Player player) {
        if (!this.nametagEnabled) {
            return;
        }
        player.setNameTag(this.nametag.replace("{player}", player.getDisplayName()));
    }
}
