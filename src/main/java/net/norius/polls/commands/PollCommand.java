package net.norius.polls.commands;

import net.norius.polls.Polls;
import net.norius.polls.poll.Poll;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record PollCommand(Polls plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length != 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigLoader().get("commands.poll.no-player"));
                return true;
            }

            // TODO: open poll menu
        } else {
            long pollId;

            try {
                pollId = Long.parseLong(args[0]);

                if(pollId <= 0) {
                    sender.sendMessage(plugin.getConfigLoader().get("commands.poll.no-number"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigLoader().get("commands.poll.no-number"));
                return true;
            }

            Optional<Poll> optionalPoll = plugin.getPollManager().getPoll(pollId);

            if(optionalPoll.isEmpty()) {
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "close" -> {

                }
                case "remove" -> {

                }
            }
        }

        return true;
    }
}
