package voidpe.chat;

import org.powernukkitx.Player;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.EventPriority;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerChatEvent;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;

import java.util.HashSet;
import java.util.Set;

/**
 * VoidChat - local chat inside a radius + global chat with a leading "!".
 *
 * Local  : message goes only to players within "radius" blocks (same world) + console.
 * Global : message starts with "!" - goes to everybody, the "!" is cut off.
 *
 * All texts are taken from plugins/VoidChat/config.yml.
 * If the file is missing, the hardcoded defaults below are used.
 */
public class VoidChat extends PluginBase implements Listener {

    private double radius;
    private String globalSymbol;
    private String formatLocal;
    private String formatGlobal;
    private boolean hintWhenAlone;
    private String hintMessage;

    @Override
    public void onEnable() {
        Config cfg = this.getConfig();

        this.radius = cfg.getInt("radius", 100);
        this.globalSymbol = cfg.getString("global-symbol", "!");

        // {%0} = player name, {%1} = message (PowerNukkitX replaces them itself)
        this.formatLocal = cfg.getString("format-local",
                "\u00a7f{%0}\u00a78 \u00bb \u00a77{%1}");
        this.formatGlobal = cfg.getString("format-global",
                "\u00a78[\u00a76G\u00a78] \u00a7f{%0}\u00a78 \u00bb \u00a77{%1}");

        this.hintWhenAlone = cfg.getBoolean("hint-when-alone", true);
        this.hintMessage = cfg.getString("hint-when-alone-message",
                "\u00a77\u0422\u0435\u0431\u044f \u043d\u0438\u043a\u0442\u043e \u043d\u0435 \u0441\u043b\u044b\u0448\u0438\u0442. \u041d\u0430\u0447\u043d\u0438 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435 \u0441 \u00a7f!\u00a77, \u0447\u0442\u043e\u0431\u044b \u043f\u0438\u0441\u0430\u0442\u044c \u0432 \u043e\u0431\u0449\u0438\u0439 \u0447\u0430\u0442");

        this.getServer().getPluginManager().registerEvents(this, this);
        this.getLogger().info("VoidChat enabled. local radius=" + (int) this.radius
                + " blocks, global symbol=\"" + this.globalSymbol + "\"");
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

        boolean global = !this.globalSymbol.isEmpty() && message.startsWith(this.globalSymbol);

        if (global) {
            message = message.substring(this.globalSymbol.length()).trim();
            if (message.isEmpty()) {
                event.setCancelled(true);
                player.sendMessage(this.hintMessage);
                return;
            }
        }

        // the "!" must not be visible in chat
        event.setMessage(message);
        event.setFormat(global ? this.formatGlobal : this.formatLocal);

        if (global) {
            return; // recipients stay default = everyone
        }

        Set<CommandSender> recipients = new HashSet<>();
        int heard = 0;
        for (Player other : player.getLevel().getPlayers().values()) {
            if (other == player) {
                continue;
            }
            if (player.distance(other) <= this.radius) {
                recipients.add(other);
                heard++;
            }
        }
        recipients.add(player);
        recipients.add(this.getServer().getConsoleSender());
        event.setRecipients(recipients);

        if (heard == 0 && this.hintWhenAlone) {
            player.sendMessage(this.hintMessage);
        }
    }
}
