package net.norius.polls.database.daos;

import net.norius.polls.Polls;
import net.norius.polls.poll.AnswerType;
import net.norius.polls.poll.ChoiceAnswer;
import net.norius.polls.poll.Poll;
import net.norius.polls.poll.PollVote;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

public record PollLoadingDAO(Polls plugin) {

    public CompletableFuture<Map<Long, Poll>> loadActivePolls() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = plugin.getDatabase().getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM polls_polls WHERE is_active = True");
                 ResultSet resultSet = statement.executeQuery()) {

                Map<Long, Poll> polls = new HashMap<>();

                while (resultSet.next()) {
                    long pollId = resultSet.getLong("poll_id");
                    polls.put(pollId, loadPoll(resultSet, connection, pollId));
                }

                return polls;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load active polls!", e);
            }

            return null;
        }, plugin.getDatabase().getExecutor());
    }

    private Poll loadPoll(ResultSet resultSet, Connection connection, long pollId) throws SQLException {
        return new Poll(
                resultSet.getString("question_text"),
                AnswerType.valueOf(resultSet.getString("answer_type")),
                loadMultipleChoices(connection, pollId),
                loadPollVotes(connection, pollId),
                resultSet.getTimestamp("created_at"),
                new Timestamp(System.currentTimeMillis() + resultSet.getLong("poll_duration")),
                resultSet.getBoolean("is_active"),
                UUID.fromString(resultSet.getString("poll_creator"))
        );
    }

    private List<String> loadMultipleChoices(Connection connection, long pollId) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM polls_choices WHERE poll_id = ?")) {
            statement.setLong(1, pollId);

            try(ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, String> choices = new HashMap<>();

                while (resultSet.next()) {
                    choices.put(resultSet.getInt("choice_order"),
                            resultSet.getString("choice_text"));
                }

                return choices.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());
            }
        }
    }

    private List<PollVote> loadPollVotes(Connection connection, long pollId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM polls_votes WHERE poll_id = ?")) {
            statement.setLong(1, pollId);

            try(ResultSet resultSet = statement.executeQuery()) {

                List<PollVote> pollVotes = new ArrayList<>();

                while(resultSet.next()) {
                    pollVotes.add(new PollVote(
                            UUID.fromString(resultSet.getString("user_id")),
                            ChoiceAnswer.valueOf(resultSet.getString("choice_answer")),
                            resultSet.getTimestamp("voted_at"),
                            resultSet.getInt("choice_order")
                    ));
                }

                return pollVotes;
            }
        }
    }
}
