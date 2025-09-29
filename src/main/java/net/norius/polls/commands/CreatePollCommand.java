package net.norius.polls.commands;

import net.norius.polls.Polls;
import net.norius.polls.gui.menus.CreatePollMenu;
import net.norius.polls.poll.PollSettings;
import net.norius.polls.utils.TimeParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record CreatePollCommand(Polls plugin) implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        final String path = "commands.createpoll.";

        if(!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigLoader().get("general.no-player"));
            return true;
        }

        if(args.length < 2) {
            player.sendMessage(plugin.getConfigLoader().get(path + "usage"));
            return true;
        }

        long durationMillis;

        try {
            durationMillis = TimeParser.parseToMillis(args[0]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getConfigLoader().get(path + "invalid-format"));
            return true;
        }

        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 1; i < args.length; i++) {
            stringBuilder.append(args[i]).append(" ");
        }

        player.openInventory(new CreatePollMenu(plugin, new PollSettings(
                stringBuilder.toString(),
                durationMillis,
                new ArrayList<>()
        )).create());
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> tab = new ArrayList<>();

        if(args.length == 1) {
            if(sender.hasPermission("polls.createpoll")) {
                tab.addAll(List.of("1w", "1d", "1h", "1m", "1s"));
            }
        }

        return tab.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
