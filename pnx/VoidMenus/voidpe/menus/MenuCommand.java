package voidpe.menus;

import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

public class MenuCommand extends Command {

    private final VoidMenus plugin;

    public MenuCommand(VoidMenus plugin) {
        super("menu");
        this.plugin = plugin;
        this.setDescription("Открыть меню сервера");
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
            return this.plugin.open(player, "main");
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("voidmenus.admin")) {
                sender.sendMessage("§cНет прав");
                return false;
            }
            this.plugin.loadMenus();
            sender.sendMessage("§aМеню перезагружены: §f" + this.plugin.menuCount());
            return true;
        }

        return this.plugin.open(player, args[0]);
    }
}
