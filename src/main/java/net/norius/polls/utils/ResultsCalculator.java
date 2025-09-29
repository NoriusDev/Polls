package net.norius.polls.utils;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.poll.Poll;
import net.norius.polls.poll.PollVote;
import net.norius.polls.poll.enums.AnswerType;
import net.norius.polls.poll.enums.ChoiceAnswer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultsCalculator {

    public static Component calculateVotedAnswer(Poll poll, Polls plugin) {
        if(poll.getAnswerType() == AnswerType.YES_NO) {
            ChoiceAnswer answer = calculateVotedChoiceAnswer(poll);
            return answer == null ? null : plugin.getConfigLoader().get("gui.poll-vote-menu.vote-" + answer.name().toLowerCase() + ".name");
        } else {
            int index = calculateVotedMultipleChoiceAnswer(poll);
            return index == -1 ? null : Component.text(poll.getMultipleChoices().get(index));
        }
    }

    public static ChoiceAnswer calculateVotedChoiceAnswer(Poll poll) {
        if(poll.getAnswerType() != AnswerType.YES_NO) return null;
        long yesVoters = poll.getPollVotes().stream().filter(pollVote -> pollVote.choiceAnswer() == ChoiceAnswer.YES).count();
        long noVoters = poll.getPollVotes().size() - yesVoters;

        if(yesVoters == noVoters) {
            return null;
        } else {
            return yesVoters > noVoters ? ChoiceAnswer.YES : ChoiceAnswer.NO;
        }
    }

    public static int calculateVotedMultipleChoiceAnswer(Poll poll) {
        if(poll.getAnswerType() != AnswerType.MULTIPLE_CHOICES) return -1;
        Map<Integer, Long> counts = poll.getPollVotes().stream()
                .collect(Collectors.groupingBy(
                        PollVote::multipleChoiceAnswer,
                        Collectors.counting()
                ));

        long max = counts.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        List<Integer> topAnswers = counts.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        boolean tie = topAnswers.size() > 1;

        if(tie || topAnswers.isEmpty()) return -1;
        return topAnswers.getFirst();
    }

    public static Double calculateVotingPercent(Poll poll, ChoiceAnswer choiceAnswer, int choiceIndex) {
        if(poll.getAnswerType() == AnswerType.YES_NO) {
            return poll.getPollVotes().isEmpty() ? 0.0 : Math.round(((double) (poll.getPollVotes().stream()
                    .filter(pollVote -> pollVote.choiceAnswer() == choiceAnswer).count() * 100 / poll.getPollVotes().size())) * 100) / 100;
        } else {
            long totalVotes = poll.getPollVotes().size();

            if (totalVotes == 0) {
                return 0.0;
            }

            long votesForAnswer = poll.getPollVotes().stream()
                    .filter(vote -> vote.multipleChoiceAnswer() == choiceIndex)
                    .count();

            double percentage = (votesForAnswer * 100.0) / totalVotes;
            return Math.round(percentage * 100.0) / 100.0;
        }
    }
}
