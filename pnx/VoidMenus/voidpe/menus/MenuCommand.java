package voidpe.menus;

import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

public class MenuCommand extends Command {

    private final VoidMenus plugin;

    public MenuCommand(VoidMenus plugin) {
        super("menu", "Открыть меню сервера", "/menu [id]");
        this.plugin = plugin;
        // второй ярлык на случай, если "menu" занят другой командой
        this.setAliases(new String[]{"menus"});
        this.setPermission("voidmenus.use");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cКоманда только для игроков");
            return false;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            return this.plugin.openMain(player);
        }

        String first = args[0].toLowerCase(java.util.Locale.ROOT);
        if (first.equals("reload")) {
            if (!sender.hasPermission("voidmenus.admin") && !sender.isOp()) {
                sender.sendMessage("§cНет прав");
                return false;
            }
            this.plugin.loadMenus();
            sender.sendMessage("§aМеню перезагружены: §f" + this.plugin.menuCount());
            return true;
        }

        if (first.equals("list")) {
            sender.sendMessage("§eМеню: §f" + String.join("§7, §f", this.plugin.menuIds()));
            return true;
        }

        return this.plugin.open(player, args[0]);
    }
}
