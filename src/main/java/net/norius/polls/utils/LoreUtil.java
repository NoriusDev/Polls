package net.norius.polls.utils;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.poll.Poll;
import net.norius.polls.poll.enums.ChoiceAnswer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LoreUtil {

    public static List<Component> buildPollLore(Player player, Polls plugin, Poll poll, long pollId) {
        List<Component> lore = new ArrayList<>();
        String path = "gui.active-polls-menu.";

        if(poll.getPollVotes().stream().anyMatch(pollVote -> pollVote.userId().equals(player.getUniqueId()))) {
            lore.addAll(plugin.getConfigLoader().getList(path + "poll.voted-lore"));
        } else {
            lore.addAll(plugin.getConfigLoader().getList(path+ "poll.not-voted-lore"));
        }

        lore.addAll(plugin.getConfigLoader().getList(path + "poll.lore",
                new String[]{"poll-id", "duration", "creation-date"},
                new Component[]{Component.text(pollId), DurationFormatter.format(poll.getEndingAt(), plugin), Component.text(
                        new SimpleDateFormat(Objects.requireNonNull(plugin.getConfig().getString("language.gui.date-format"))).format(poll.getCreatedAt())
                )}));

        return lore;
    }

    public static List<Component> buildResultsLore(boolean showResults, Polls plugin, Poll poll, String path, int index, ChoiceAnswer answer) {
        List<Component> lore;

        if(showResults) {
            if((answer == null && ResultsCalculator.calculateVotingPercent(poll, null, index) == 0) ||
                    (answer != null && ResultsCalculator.calculateVotingPercent(poll, answer, 0) == 0)) {
                lore = plugin.getConfigLoader().getList(path + "result.no-votes-lore");
            } else {
                lore = new ArrayList<>(plugin.getConfigLoader().getList(path + "result.lore"));

                poll.getPollVotes().stream().filter(pollVote -> {
                            if(answer == null) {
                                return pollVote.multipleChoiceAnswer() == index;
                            } else {
                                return pollVote.choiceAnswer() == answer;
                            }
                        })
                        .forEach(pollVote -> lore.add(plugin.getConfigLoader().get(path + "result.voter", new String[]{"player", "vote-date"},
                                new Component[]{Component.text(
                                        Objects.requireNonNull(Bukkit.getOfflinePlayer(pollVote.userId()).getName())),
                                        Component.text(new SimpleDateFormat(Objects.requireNonNull(plugin.getConfig().getString("language.gui.date-format"))).format(pollVote.votedAt()))})));
            }
        } else {
            lore = plugin.getConfigLoader().getList(path + "multiple-choice.lore");
        }

        return lore;
    }
}
