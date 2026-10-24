package voidpe.menus;

import org.powernukkitx.Player;
import org.powernukkitx.event.inventory.ItemStackRequestActionEvent;
import org.powernukkitx.form.element.simple.ButtonImage;
import org.powernukkitx.form.window.SimpleForm;
import org.powernukkitx.inventory.Inventory;
import org.powernukkitx.inventory.fake.FakeInventory;
import org.powernukkitx.inventory.fake.FakeInventoryType;
import org.powernukkitx.item.Item;
import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx.utils.Config;
import org.powernukkitx.utils.ConfigSection;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * VoidMenus - меню в стиле DeluxeMenus для Bedrock-сервера VoidPE.
 *
 * Главное: меню это настоящий сундук (как на Java-серверах), предметы с названием
 * и описанием, клик по предмету запускает команды.
 *
 * Команда: /menu [id]   /menu reload   /menu list
 * Файлы  : plugins/VoidMenus/menus/*.yml  (один файл = одно меню)
 *
 * Формат файла (совместим с DeluxeMenus):
 *   menu_title: '&#00F15EМеню сервера'
 *   open_command: menu
 *   size: 54
 *   open_commands: [ '[sound] ...' ]
 *   items:
 *     Инфо:
 *       slots: [13]
 *       material: BOOK
 *       display_name: '&7Информация'
 *       lore: [ '', '&fСтрока' ]
 *       click_commands: [ '[player] warp mine', '[close]' ]
 *
 * Метки: {player} %player_name% {online} {max} {tps} {ping} {motd} {world}
 * Плюс PlaceholderAPI, если он установлен (по рефлексии, без зависимости).
 */
public class VoidMenus extends PluginBase {

    private final Map<String, Menu> menus = new LinkedHashMap<>();
    private final Set<String> registeredCommands = new LinkedHashSet<>();
    private File menusDir;
    private File legacyFile;
    private Object placeholderApi;
    private Method placeholderProcess;

    // ------------------------------------------------------------------ enable

    @Override
    public void onEnable() {
        try {
            File data = this.getDataFolder();
            data.mkdirs();
            this.menusDir = new File(data, "menus");
            this.menusDir.mkdirs();
            this.legacyFile = new File(data, "menus.yml");
            if (this.countMenuFiles() == 0) {
                // первый запуск: кладём готовое меню из jar, чтобы /menu работал сразу
                this.saveResource("menus/menu.yml", "menus/menu.yml", false);
            }
        } catch (Throwable t) {
            this.getLogger().error("VoidMenus: не удалось создать папку меню: " + t);
        }

        try {
            this.loadMenus();
        } catch (Throwable t) {
            this.getLogger().error("VoidMenus: ошибка чтения меню (проверь отступы в yml): " + t);
        }

        this.registerCommands();

        this.getLogger().info("VoidMenus enabled. version=2.0.0"
                + ", меню=" + this.menus.size()
                + ", главное=" + (this.resolve("menu") != null ? "есть" : "нет"));
    }

    @Override
    public void onDisable() {
        this.menus.clear();
    }

    // ------------------------------------------------------------------ config

    public void loadMenus() {
        this.menus.clear();
        List<File> files = new ArrayList<>();
        File[] found = this.menusDir == null ? null : this.menusDir.listFiles();
        if (found != null) {
            for (File file : found) {
                if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                    files.add(file);
                }
            }
        }
        files.sort(Comparator.comparing(File::getName));

        for (File file : files) {
            try {
                Menu menu = this.loadFile(file, baseName(file.getName()));
                if (menu != null) {
                    this.register(menu);
                }
            } catch (Throwable t) {
                this.getLogger().error("VoidMenus: файл " + file.getName() + " не прочитан: " + t);
            }
        }

        if (files.isEmpty() && this.legacyFile != null && this.legacyFile.exists()) {
            this.getLogger().warning("VoidMenus: найден старый menus.yml, читаю его. "
                    + "Новые меню - это папка menus\\*.yml");
            try {
                this.loadLegacy(new Config(this.legacyFile, Config.YAML));
            } catch (Throwable t) {
                this.getLogger().error("VoidMenus: старый menus.yml не прочитан: " + t);
            }
        }

        if (this.menus.isEmpty()) {
            this.getLogger().warning("VoidMenus: меню не найдено. Положи файлы в plugins/VoidMenus/menus/*.yml");
        }
    }

    private Menu loadFile(File file, String id) {
        Config cfg = new Config(file, Config.YAML);

        Menu menu = new Menu();
        menu.id = id;
        menu.title = cfg.getString("menu_title", cfg.getString("title", id));
        menu.permission = cfg.getString("permission", "");
        menu.size = clampSize(cfg.getInt("size", 54));
        menu.content = cfg.getString("content", "");
        menu.openCommands.addAll(asStringList(cfg.get("open_command")));
        menu.openCommands.addAll(asStringList(cfg.get("open_commands_alias")));

        Object openActions = cfg.get("open_commands");
        if (openActions instanceof List) {
            for (Object raw : (List<?>) openActions) {
                Menu.Action action = parseAction(stringOf(raw));
                if (action != null) {
                    menu.openActions.add(action);
                }
            }
        }

        ConfigSection items = cfg.getSection("items");
        if (items != null && !items.isEmpty()) {
            int next = 0;
            for (String name : items.keySet()) {
                if ("true".equalsIgnoreCase(stringOf(cfg.get("items." + name)))) {
                    continue;
                }
                ConfigSection section = items.getSection(name);
                if (section == null) {
                    continue;
                }
                Menu.MenuItem item = new Menu.MenuItem();
                item.name = name;
                item.material = section.getString("material", "PAPER");
                item.amount = section.getInt("amount", section.getInt("count", 1));
                item.displayName = section.getString("display_name",
                        section.getString("name", name));
                item.lore.addAll(asStringList(section.get("lore")));
                item.slots.addAll(parseSlots(section.get("slots"), next));
                for (String raw : asStringList(section.get("click_commands"))) {
                    Menu.Action action = parseAction(raw);
                    if (action != null) {
                        item.actions.add(action);
                    }
                }
                for (String raw : asStringList(section.get("actions"))) {
                    Menu.Action action = parseAction(raw);
                    if (action != null) {
                        item.actions.add(action);
                    }
                }
                for (int slot : item.slots) {
                    next = Math.max(next, slot + 1);
                }
                menu.items.add(item);
            }
        }

        ConfigSection buttons = cfg.getSection("buttons");
        if (buttons != null && !buttons.isEmpty()) {
            menu.form = menu.items.isEmpty();
            for (String name : buttons.keySet()) {
                ConfigSection section = buttons.getSection(name);
                if (section == null) {
                    continue;
                }
                Menu.Button button = new Menu.Button();
                button.text = section.getString("text", name);
                button.image = section.getString("image", "");
                button.permission = section.getString("permission", "");
                for (String raw : asStringList(section.get("actions"))) {
                    Menu.Action action = parseAction(raw);
                    if (action != null) {
                        button.actions.add(action);
                    }
                }
                menu.buttons.add(button);
            }
        }

        if (menu.items.isEmpty() && menu.buttons.isEmpty()) {
            this.getLogger().warning("VoidMenus: в " + file.getName() + " нет секции items — меню пропущено");
            return null;
        }
        return menu;
    }

    /** старый формат: menus.yml с секцией menus.<id>.buttons */
    private void loadLegacy(Config config) {
        ConfigSection root = config.getSection("menus");
        if (root == null || root.isEmpty()) {
            return;
        }
        for (String id : root.keySet()) {
            String base = "menus." + id;
            Menu menu = new Menu();
            menu.id = id;
            menu.form = true;
            menu.title = config.getString(base + ".title", id);
            menu.content = config.getString(base + ".content", "");
            menu.permission = config.getString(base + ".permission", "");
            for (Map<?, ?> raw : config.getMapList(base + ".buttons")) {
                Menu.Button button = new Menu.Button();
                button.text = stringOf(raw.get("text"));
                button.image = stringOf(raw.get("image"));
                button.permission = stringOf(raw.get("permission"));
                Object actions = raw.get("actions");
                if (actions instanceof List) {
                    for (Object entry : (List<?>) actions) {
                        Menu.Action action = parseAction(stringOf(entry));
                        if (action != null) {
                            button.actions.add(action);
                        }
                    }
                }
                menu.buttons.add(button);
            }
            this.register(menu);
        }
    }

    /** добавить готовое меню (используется загрузчиком и тестами) */
    public void register(Menu menu) {
        if (menu == null || menu.id == null || menu.id.isEmpty()) {
            return;
        }
        this.menus.put(menu.id.toLowerCase(Locale.ROOT), menu);
    }

    private int countMenuFiles() {
        File[] files = this.menusDir == null ? null : this.menusDir.listFiles();
        int count = 0;
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                    count++;
                }
            }
        }
        return count;
    }

    public int menuCount() {
        return this.menus.size();
    }

    public Set<String> menuIds() {
        return this.menus.keySet();
    }

    // ------------------------------------------------------------------ command

    private void registerCommands() {
        if (this.registeredCommands.add("menu")) {
            try {
                boolean ok = this.getServer().getCommandMap().register("voidmenus", new MenuCommand(this));
                this.getLogger().info("VoidMenus: /menu зарегистрирована = " + ok);
            } catch (Throwable t) {
                this.getLogger().error("VoidMenus: не удалось зарегистрировать /menu: " + t);
            }
        }
        for (Menu menu : new ArrayList<>(this.menus.values())) {
            for (String raw : menu.openCommands) {
                String name = commandName(raw);
                if (name.isEmpty() || name.equals("menu") || !this.registeredCommands.add(name)) {
                    continue;
                }
                try {
                    this.getServer().getCommandMap().register("voidmenus", new OpenMenuCommand(this, name, menu.id));
                } catch (Throwable t) {
                    this.getLogger().warning("VoidMenus: команда /" + name + " не зарегистрирована: " + t);
                }
            }
        }
    }

    private static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);
    }

    // -------------------------------------------------------------------- open

    public Menu resolve(String id) {
        if (id == null) {
            return null;
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        Menu direct = this.menus.get(key);
        if (direct != null) {
            return direct;
        }
        for (Menu menu : this.menus.values()) {
            for (String command : menu.openCommands) {
                if (commandName(command).equals(key)) {
                    return menu;
                }
            }
        }
        return null;
    }

    public boolean openMain(Player player) {
        Menu menu = this.resolve("menu");
        if (menu == null && !this.menus.isEmpty()) {
            menu = this.menus.values().iterator().next();
        }
        return menu != null && this.open(player, menu.id);
    }

    public boolean open(Player player, String id) {
        try {
            return this.openInternal(player, id);
        } catch (Throwable t) {
            // исключение внутри тика игрока роняет весь сервер - ловим всё
            this.getLogger().error("VoidMenus: ошибка при открытии меню '" + id + "': " + t);
            try {
                player.sendMessage("\u00a7cМеню сломалось, ошибка записана в лог");
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    private boolean openInternal(Player player, String id) {
        if (player == null) {
            return false;
        }
        Menu menu = this.resolve(id);
        if (menu == null) {
            player.sendMessage("\u00a7cМеню \u00a7f" + id + "\u00a7c не найдено");
            return false;
        }
        String permission = menu.permission;
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            player.sendMessage("\u00a7cУ тебя нет доступа к этому меню");
            return false;
        }

        this.closeTopWindow(player);
        this.runActions(player, menu, menu.openActions, null);

        if (menu.form) {
            return this.openForm(player, menu);
        }
        return this.openGui(player, menu);
    }

    private void closeTopWindow(Player player) {
        try {
            Optional<Inventory> top = player.getTopWindow();
            if (top.isPresent()) {
                player.removeWindow(top.get());
            }
        } catch (Throwable t) {
            this.getLogger().warning("VoidMenus: не удалось закрыть прошлое окно: " + t);
        }
    }

    // --------------------------------------------------------------- сундук GUI

    private boolean openGui(Player player, Menu menu) {
        FakeInventoryType type = menu.size > 27 ? FakeInventoryType.DOUBLE_CHEST : FakeInventoryType.CHEST;
        FakeInventory inventory = new FakeInventory(type, Text.colorize(this.render(player, menu.title)));
        this.fill(player, menu, inventory);
        inventory.setDefaultItemHandler((inv, slot, oldItem, newItem, event) -> this.cancel(event));
        int windowId = player.addWindow(inventory);
        if (windowId < 0) {
            player.sendMessage("\u00a7cМеню не открылось: закрой инвентарь и попробуй снова");
            return false;
        }
        return true;
    }

    private void fill(Player player, Menu menu, FakeInventory inventory) {
        int size = inventory.getSize();
        for (Menu.MenuItem item : menu.items) {
            Item stack = this.buildItem(player, item);
            for (int slot : item.slots) {
                if (slot < 0 || slot >= size) {
                    continue;
                }
                if (stack == null) {
                    continue;
                }
                inventory.setItem(slot, stack.clone());
                inventory.setItemHandler(slot, (inv, clickedSlot, oldItem, newItem, event) -> {
                    this.cancel(event);
                    this.click(player, menu, item, inv);
                });
            }
        }
    }

    private void cancel(ItemStackRequestActionEvent event) {
        if (event != null) {
            event.setCancelled(true);
        }
    }

    private void click(Player player, Menu menu, Menu.MenuItem item, FakeInventory inventory) {
        boolean refresh = false;
        try {
            for (Menu.Action action : item.actions) {
                String type = action.type;
                if (type.equals("player")) {
                    this.execute(player, this.render(player, action.value));
                } else if (type.equals("console")) {
                    this.execute(this.getServer().getConsoleSender(), this.render(player, action.value));
                } else if (type.equals("menu") || type.equals("openguimenu") || type.equals("openmenu")) {
                    this.open(player, this.render(player, action.value));
                    return;
                } else if (type.equals("message")) {
                    player.sendMessage(Text.colorize(this.render(player, action.value)));
                } else if (type.equals("close")) {
                    this.closeTopWindow(player);
                    return;
                } else if (type.equals("refresh")) {
                    refresh = true;
                }
                // sound / broadcast / givepermission и прочее из DeluxeMenus просто игнорируем
            }
            if (refresh && player.getTopWindow().orElse(null) == inventory) {
                this.fill(player, menu, inventory);
                inventory.sendContents(player);
            }
        } catch (Throwable t) {
            this.getLogger().error("VoidMenus: ошибка в действии кнопки '" + item.name + "': " + t);
        }
    }

    private Item buildItem(Player player, Menu.MenuItem item) {
        Item stack = this.createItem(this.render(player, item.material));
        if (stack == null) {
            return null;
        }
        if (item.amount > 1) {
            stack.setCount(Math.min(item.amount, 64));
        }
        String name = item.displayName == null ? "" : item.displayName;
        if (!name.isEmpty()) {
            stack.setCustomName(Text.colorize(this.render(player, name)));
        }
        if (!item.lore.isEmpty()) {
            List<String> lines = new ArrayList<>(item.lore.size());
            for (String line : item.lore) {
                lines.add(Text.colorize(this.render(player, line)));
            }
            stack.setLore(lines.toArray(new String[0]));
        }
        return stack;
    }

    private Item createItem(String material) {
        String raw = material == null ? "" : material.trim();
        if (raw.isEmpty()) {
            raw = "paper";
        }
        String id = raw.toLowerCase(Locale.ROOT);
        int meta = 0;
        if (id.startsWith("head") || id.startsWith("skull") || id.equals("player_head")) {
            id = "minecraft:skull";
            meta = 3; // голова игрока
        } else if (!id.contains(":")) {
            id = "minecraft:" + id.replace(' ', '_');
        }
        try {
            Item stack = Item.get(id, meta, 1);
            if (stack != null) {
                return stack;
            }
        } catch (Throwable ignored) {
        }
        this.getLogger().warning("VoidMenus: неизвестный material '" + raw + "' — ставлю бумагу");
        try {
            return Item.get("minecraft:paper", 0, 1);
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------- форма

    private boolean openForm(Player player, Menu menu) {
        SimpleForm form = new SimpleForm(Text.colorize(this.render(player, menu.title)),
                Text.colorize(this.render(player, menu.content)));
        for (Menu.Button button : menu.buttons) {
            if (button.permission != null && !button.permission.isEmpty()
                    && !player.hasPermission(button.permission)) {
                continue;
            }
            String text = Text.colorize(this.render(player, button.text));
            ButtonImage image = this.parseImage(button.image);
            if (image == null) {
                form.addButton(text, p -> this.runActions(p, menu, button.actions, form));
            } else {
                form.addButton(text, image, p -> this.runActions(p, menu, button.actions, form));
            }
        }
        form.send(player);
        return true;
    }

    private static ButtonImage parseImage(String value) {
        String path = value == null ? "" : value.trim();
        if (path.isEmpty()) {
            return null;
        }
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return ButtonImage.Type.URL.of(path);
            }
            return ButtonImage.Type.PATH.of(path);
        } catch (Throwable t) {
            return null;
        }
    }

    private void runActions(Player player, Menu menu, List<Menu.Action> actions, SimpleForm form) {
        for (Menu.Action action : actions) {
            try {
                String value = this.render(player, action.value);
                String type = action.type;
                if (type.equals("player")) {
                    this.execute(player, value);
                } else if (type.equals("console")) {
                    this.execute(this.getServer().getConsoleSender(), value);
                } else if (type.equals("menu") || type.equals("openguimenu") || type.equals("openmenu")) {
                    this.open(player, value);
                    return;
                } else if (type.equals("message")) {
                    player.sendMessage(Text.colorize(value));
                } else if (type.equals("close")) {
                    // в форме кнопка и так закрывает окно, для сундука - см. click()
                }
            } catch (Throwable t) {
                this.getLogger().error("VoidMenus: ошибка в действии '" + action.type + " "
                        + action.value + "': " + t);
            }
        }
    }

    private void execute(org.powernukkitx.command.CommandSender sender, String command) {
        String line = command == null ? "" : command.trim();
        if (line.startsWith("/")) {
            line = line.substring(1);
        }
        if (line.isEmpty()) {
            return;
        }
        this.getServer().getCommandMap().executeCommand(sender, line);
    }

    // -------------------------------------------------------------- текст/метки

    public String render(Player player, String text) {
        if (text == null) {
            return "";
        }
        String name = player.getName();
        String out = text
                .replace("%player_name%", name)
                .replace("%player_displayname%", player.getDisplayName())
                .replace("%player%", name)
                .replace("{player}", name)
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

    // ------------------------------------------------------------------ парсеры

    /** '[player] spawn' или старый вид 'player: spawn' */
    public static Menu.Action parseAction(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.startsWith("[")) {
            int end = text.indexOf(']');
            if (end > 1) {
                String type = text.substring(1, end).trim();
                String value = text.substring(end + 1).trim();
                if (value.startsWith("/")) {
                    value = value.substring(1);
                }
                return new Menu.Action(type, value);
            }
            return null;
        }
        int colon = text.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        return new Menu.Action(text.substring(0, colon), text.substring(colon + 1).trim());
    }

    public static List<Integer> parseSlots(Object value, int fallback) {
        List<Integer> slots = new ArrayList<>();
        if (value instanceof Number) {
            slots.add(((Number) value).intValue());
        } else if (value instanceof List) {
            for (Object entry : (List<?>) value) {
                slots.addAll(parseSlots(entry, -1));
            }
        } else if (value != null) {
            String text = String.valueOf(value).trim();
            if (text.startsWith("[") && text.endsWith("]")) {
                text = text.substring(1, text.length() - 1);
            }
            if (text.startsWith("{") && text.endsWith("}")) {
                text = text.substring(1, text.length() - 1);
            }
            int dash = text.indexOf('-');
            if (dash > 0) {
                try {
                    int from = Integer.parseInt(text.substring(0, dash).trim());
                    int to = Integer.parseInt(text.substring(dash + 1).trim());
                    for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
                        slots.add(i);
                    }
                } catch (NumberFormatException ignored) {
                    // не диапазон
                }
            } else {
                for (String part : text.split("[, ]+")) {
                    if (part.isEmpty()) {
                        continue;
                    }
                    try {
                        slots.add(Integer.parseInt(part.trim()));
                    } catch (NumberFormatException ignored) {
                        // мусор
                    }
                }
            }
        }
        if (slots.isEmpty() && fallback >= 0) {
            slots.add(fallback);
        }
        return slots;
    }

    public static List<String> asStringList(Object value) {
        List<String> list = new ArrayList<>();
        if (value instanceof List) {
            for (Object entry : (List<?>) value) {
                if (entry != null) {
                    list.add(String.valueOf(entry));
                }
            }
        } else if (value != null) {
            list.add(String.valueOf(value));
        }
        return list;
    }

    private static String commandName(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("/")) {
            text = text.substring(1);
        }
        int space = text.indexOf(' ');
        if (space > 0) {
            text = text.substring(0, space);
        }
        return text.toLowerCase(Locale.ROOT);
    }

    private static int clampSize(int size) {
        if (size <= 9) {
            return 9;
        }
        if (size <= 18) {
            return 18;
        }
        if (size <= 27) {
            return 27;
        }
        if (size <= 36) {
            return 36;
        }
        if (size <= 45) {
            return 45;
        }
        return 54;
    }

    private static String stringOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
