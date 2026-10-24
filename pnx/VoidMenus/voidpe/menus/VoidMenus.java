package voidpe.menus;

import org.powernukkitx.Player;
import org.powernukkitx.form.element.simple.ButtonImage;
import org.powernukkitx.form.window.SimpleForm;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;
import org.powernukkitx.utils.ConfigSection;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * VoidMenus - DeluxeMenus-like menus for Bedrock players (native forms).
 *
 * Command: /menu [id]   /menu reload (voidmenus.admin)
 * Config : plugins/VoidMenus/menus.yml
 *
 * Tokens: {player} {online} {max} {tps} {ping} {motd} {world}
 * PlaceholderAPI placeholders are supported when the plugin is installed.
 */
public class VoidMenus extends PluginBase {

    private final Map<String, Menu> menus = new LinkedHashMap<>();
    private File menusFile;
    private Object placeholderApi;
    private Method placeholderProcess;

    @Override
    public void onEnable() {
        try {
            this.getDataFolder().mkdirs();
            this.menusFile = new File(this.getDataFolder(), "menus.yml");
            if (!this.menusFile.exists()) {
                this.saveResource("menus.yml", false);
            }
        } catch (Throwable t) {
            this.getLogger().error("VoidMenus: не удалось создать menus.yml: " + t);
        }

        try {
            this.loadMenus();
        } catch (Throwable t) {
            this.getLogger().error("VoidMenus: ошибка в menus.yml (проверь отступы и кавычки): " + t);
        }

        try {
            boolean registered = this.getServer().getCommandMap().register("voidmenus", new MenuCommand(this));
            this.getLogger().info("VoidMenus enabled. menus=" + this.menus.size()
                    + ", menu=" + (this.menus.containsKey("main") ? "main" : "нет главного меню")
                    + ", command registered=" + registered);
        } catch (Throwable t) {
            this.getLogger().error("VoidMenus: не удалось зарегистрировать команду /menu: " + t);
        }
    }

    // ------------------------------------------------------------------ config

    public void loadMenus() {
        this.loadMenus(new Config(this.menusFile, Config.YAML));
    }

    public void loadMenus(Config config) {
        this.menus.clear();
        ConfigSection root = config.getSection("menus");
        if (root == null) {
            this.getLogger().warning("menus.yml: секция 'menus' не найдена");
            return;
        }
        for (String id : root.keySet()) {
            String base = "menus." + id;
            Menu menu = new Menu();
            menu.title = config.getString(base + ".title", id);
            menu.content = config.getString(base + ".content", "");
            menu.permission = config.getString(base + ".permission", "");
            for (Map<?, ?> raw : config.getMapList(base + ".buttons")) {
                Button button = new Button();
                button.text = string(raw.get("text"));
                button.image = string(raw.get("image"));
                button.permission = string(raw.get("permission"));
                Object actions = raw.get("actions");
                if (actions instanceof List) {
                    for (Object entry : (List<?>) actions) {
                        Action action = parseAction(string(entry));
                        if (action != null) {
                            button.actions.add(action);
                        }
                    }
                }
                menu.buttons.add(button);
            }
            this.menus.put(id.toLowerCase(Locale.ROOT), menu);
        }
    }

    public int menuCount() {
        return this.menus.size();
    }

    // -------------------------------------------------------------------- open

    public boolean open(Player player, String id) {
        try {
            return this.openMenu(player, id);
        } catch (Throwable t) {
            // исключение внутри тика роняет весь сервер - ловим всё
            this.getLogger().error("VoidMenus: ошибка при открытии меню '" + id + "': " + t);
            try {
                player.sendMessage("\u00a7cМеню сломалось, ошибка записана в лог");
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    private boolean openMenu(Player player, String id) {
        Menu menu = this.menus.get(id.toLowerCase(Locale.ROOT));
        if (menu == null) {
            player.sendMessage("\u00a7cМеню \u00a7f" + id + "\u00a7c не найдено");
            return false;
        }
        if (!menu.permission.isEmpty() && !player.hasPermission(menu.permission)) {
            player.sendMessage("\u00a7cУ тебя нет доступа к этому меню");
            return false;
        }
        SimpleForm form = new SimpleForm(this.render(player, menu.title), this.render(player, menu.content));
        for (Button button : menu.buttons) {
            if (!button.permission.isEmpty() && !player.hasPermission(button.permission)) {
                continue;
            }
            String text = this.render(player, button.text);
            ButtonImage image = this.image(button.image);
            List<Action> actions = button.actions;
            if (image == null) {
                form.addButton(text, p -> this.runActions(p, actions));
            } else {
                form.addButton(text, image, p -> this.runActions(p, actions));
            }
        }
        form.send(player);
        return true;
    }

    private void runActions(Player player, List<Action> actions) {
        for (Action action : actions) {
            try {
                String value = this.render(player, action.value);
                switch (action.type) {
                    case "player":
                        this.getServer().getCommandMap().executeCommand(player, value);
                        break;
                    case "console":
                        this.getServer().getCommandMap().executeCommand(this.getServer().getConsoleSender(), value);
                        break;
                    case "menu":
                        this.open(player, value);
                        break;
                    case "message":
                        player.sendMessage(value);
                        break;
                    default:
                        break; // close: форма закрывается сама
                }
            } catch (Throwable t) {
                this.getLogger().error("VoidMenus: ошибка в действии '" + action.type + " " + action.value + "': " + t);
            }
        }
    }

    private ButtonImage image(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return ButtonImage.Type.URL.of(path);
        }
        return ButtonImage.Type.PATH.of(path);
    }

    private static Action parseAction(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.equalsIgnoreCase("close")) {
            return new Action("close", "");
        }
        int index = text.indexOf(':');
        if (index < 0) {
            return null;
        }
        return new Action(text.substring(0, index).trim().toLowerCase(Locale.ROOT),
                text.substring(index + 1).trim());
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // ------------------------------------------------------------- placeholders

    public String render(Player player, String text) {
        if (text == null) {
            return "";
        }
        String out = text
                .replace("{player}", player.getName())
                .replace("{online}", String.valueOf(this.getServer().getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(this.getServer().getMaxPlayers()))
                .replace("{tps}", String.format(Locale.ROOT, "%.1f", this.getServer().getTicksPerSecond()))
                .replace("{ping}", String.valueOf(player.getPing()))
                .replace("{motd}", String.valueOf(this.getServer().getMotd()))
                .replace("{world}", player.getLevel().getName());
        return this.applyPlaceholders(player, out);
    }

    private void hookPlaceholderAPI() {
        try {
            Class<?> api = Class.forName("org.powernukkitx.placeholderapi.PlaceholderAPI");
            Object instance = api.getMethod("get").invoke(null);
            if (instance == null) {
                return;
            }
            this.placeholderProcess = api.getMethod("processPlaceholders", Player.class, String.class);
            this.placeholderApi = instance;
        } catch (Throwable ignored) {
            this.placeholderApi = null;
            this.placeholderProcess = null;
        }
    }

    private String applyPlaceholders(Player player, String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        if (this.placeholderProcess == null) {
            this.hookPlaceholderAPI();
            if (this.placeholderProcess == null) {
                return text;
            }
        }
        try {
            return (String) this.placeholderProcess.invoke(this.placeholderApi, player, text);
        } catch (Throwable t) {
            return text;
        }
    }

    // ------------------------------------------------------------------ models

    public static class Menu {
        public String title = "";
        public String content = "";
        public String permission = "";
        public final List<Button> buttons = new ArrayList<>();
    }

    public static class Button {
        public String text = "";
        public String image = "";
        public String permission = "";
        public final List<Action> actions = new ArrayList<>();
    }

    public static class Action {
        public final String type;
        public final String value;

        public Action(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}
