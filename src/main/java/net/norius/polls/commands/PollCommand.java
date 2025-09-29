package net.norius.polls.commands;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.gui.menus.ActivePollsMenu;
import net.norius.polls.poll.Poll;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record PollCommand(Polls plugin) implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        String path = "commands.poll.";

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigLoader().get("general.no-player"));
                return true;
            }

            player.openInventory(new ActivePollsMenu(plugin, player).create());
        } else if(args.length == 2) {
            long pollId;

            try {
                pollId = Long.parseLong(args[1]);

                if (pollId <= 0) {
                    sender.sendMessage(plugin.getConfigLoader().get(path + "no-number"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigLoader().get(path + "no-number"));
                return true;
            }

            Optional<Poll> optionalPoll = plugin.getPollManager().getPoll(pollId);

            if (optionalPoll.isEmpty()) {
                sender.sendMessage(plugin.getConfigLoader().get(path + "invalid-number"));
                return true;
            }

            Poll poll = optionalPoll.get();

            switch (args[0].toLowerCase()) {
                case "close" -> {
                    if (!sender.hasPermission("polls.close")) {
                        sender.sendMessage(plugin.getConfigLoader().get("general.no-perms"));
                        return true;
                    }

                    if (!poll.isActive()) {
                        sender.sendMessage(plugin.getConfigLoader().get(path + "already-closed"));
                        return true;
                    }

                    plugin.getPollManager().closeActivePoll(pollId, poll);
                    sender.sendMessage(plugin.getConfigLoader().get(path + "closed",
                            new String[]{"id"}, new Component[]{Component.text(pollId)}));
                }
                case "remove" -> {
                    if (!sender.hasPermission("polls.close")) {
                        sender.sendMessage(plugin.getConfigLoader().get("general.no-perms"));
                        return true;
                    }

                    plugin.getPollManager().removePoll(pollId);
                    sender.sendMessage(plugin.getConfigLoader().get(path + "removed",
                            new String[]{"id"}, new Component[]{Component.text(pollId)}));
                }
                default -> sender.sendMessage(plugin.getConfigLoader().get(path + "usage"));
            }
        }

        return true;
    }


    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> tab = new ArrayList<>();

        if(args.length == 1) {
            if(sender.hasPermission("polls.remove")) {
                tab.add("remove");
            }
            if(sender.hasPermission("polls.close")) {
                tab.add("close");
            }
        }

        return tab.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
