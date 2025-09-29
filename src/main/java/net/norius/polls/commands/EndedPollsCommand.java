package net.norius.polls.commands;

import net.norius.polls.Polls;
import net.norius.polls.gui.menus.EndedPollsMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record EndedPollsCommand(Polls plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigLoader().get("general.no-player"));
            return true;
        }

        player.openInventory(new EndedPollsMenu(plugin).create());
        return true;
    }
}
