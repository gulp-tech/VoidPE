package voidpe.menus;

import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

/** Команда из open_command в файле меню, например /warp или /shop. */
public class OpenMenuCommand extends Command {

    private final VoidMenus plugin;
    private final String menuId;

    public OpenMenuCommand(VoidMenus plugin, String name, String menuId) {
        super(name, "Открыть меню " + menuId, "/" + name);
        this.plugin = plugin;
        this.menuId = menuId;
        this.setPermission("voidmenus.use");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cКоманда только для игроков");
            return false;
        }
        return this.plugin.open((Player) sender, this.menuId);
    }
}
