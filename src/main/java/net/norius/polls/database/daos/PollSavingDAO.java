package net.norius.polls.database.daos;

import lombok.RequiredArgsConstructor;
import net.norius.polls.Polls;
import net.norius.polls.poll.Poll;
import net.norius.polls.poll.PollVote;
import net.norius.polls.poll.enums.AnswerType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@RequiredArgsConstructor
public class PollSavingDAO {

    private final Polls plugin;
    private final String voteSql = "INSERT INTO polls_votes(poll_id, choice_order, user_id, choice_answer, voted_at) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "choice_answer = VALUES(choice_answer), " +
            "voted_at = VALUES(voted_at)";

    public CompletableFuture<Void> saveActivePolls(Set<Map.Entry<Long, Poll>> activePolls) {
        return CompletableFuture.runAsync(() -> {
            try(Connection connection = plugin.getDatabase().getConnection()) {
                connection.setAutoCommit(false);
                try(PreparedStatement statement = connection.prepareStatement(
                        "UPDATE polls_polls SET poll_duration = ? WHERE poll_id = ?")) {

                    for (Map.Entry<Long, Poll> activePoll : activePolls) {
                        statement.setLong(1, activePoll.getValue().getEndingAt().getTime() - System.currentTimeMillis());
                        statement.setLong(2, activePoll.getKey());
                        statement.addBatch();
                    }

                    statement.executeBatch();
                    connection.commit();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save active polls", e);
            }
        }, plugin.getDatabase().getExecutor());
    }

    public void saveLastPollId(long id) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = plugin.getDatabase().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO polls_id (id, last_poll_id) VALUES (1, ?)" +
                                 "ON DUPLICATE KEY UPDATE last_poll_id = VALUES(last_poll_id);")) {
                statement.setLong(1, id);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save last poll id!", e);
            }
        }, plugin.getDatabase().getExecutor());
    }

    public void savePoll(long pollId, Poll poll) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = plugin.getDatabase().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO polls_polls(poll_id, question_text, answer_type, created_at, poll_duration, is_active, poll_creator) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE " +
                                 "question_text = VALUES(question_text), " +
                                 "answer_type = VALUES(answer_type), " +
                                 "created_at = VALUES(created_at), " +
                                 "poll_duration = VALUES(poll_duration), " +
                                 "is_active = VALUES(is_active), " +
                                 "poll_creator = VALUES(poll_creator)")) {

                try {
                    connection.setAutoCommit(false);

                    statement.setLong(1, pollId);
                    statement.setString(2, poll.getQuestion());
                    statement.setString(3, poll.getAnswerType().name());
                    statement.setTimestamp(4, poll.getCreatedAt());
                    statement.setLong(5, poll.getEndingAt().getTime() - System.currentTimeMillis());
                    statement.setBoolean(6, poll.isActive());
                    statement.setString(7, poll.getCreator().toString());
                    statement.executeUpdate();

                    savePollChoices(pollId, poll, connection);
                    saveVotes(pollId, poll, connection);

                    connection.commit();
                } catch (SQLException e) {
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        plugin.getLogger().log(Level.SEVERE, "Rollback failed!", ex);
                    }
                    plugin.getLogger().log(Level.SEVERE, "Could not save poll", e);
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save poll", e);
            }
        }, plugin.getDatabase().getExecutor());
    }

    private void savePollChoices(long pollId, Poll poll, Connection connection) throws SQLException {
        if(poll.getAnswerType() == AnswerType.YES_NO) return;

        try(PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO polls_choices(poll_id, choice_order, choice_text) VALUES(?, ?, ?)")) {

            for(int i = 0; i < poll.getMultipleChoices().size(); i++) {
                statement.setLong(1, pollId);
                statement.setInt(2, i);
                statement.setString(3, poll.getMultipleChoices().get(i));
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }


    public void saveVote(long pollId, PollVote pollVote) {
        CompletableFuture.runAsync(() -> {
            try(Connection connection = plugin.getDatabase().getConnection();
                PreparedStatement statement = connection.prepareStatement(voteSql)) {

                insertVote(pollId, pollVote, statement);
                statement.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save poll vote", e);
            }
        }, plugin.getDatabase().getExecutor());
    }

    public void removePoll(long pollId) {
        CompletableFuture.runAsync(() -> {
            try(Connection connection = plugin.getDatabase().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM polls_polls WHERE poll_id = ?")) {

                statement.setLong(1, pollId);
                statement.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove poll with id " + pollId, e);
            }
        }, plugin.getDatabase().getExecutor());
    }

    private void saveVotes(long pollId, Poll poll, Connection connection) throws SQLException {
        try(PreparedStatement statement = connection.prepareStatement(voteSql)) {
            connection.setAutoCommit(false);

            for(PollVote vote : poll.getPollVotes()) {
                insertVote(pollId, vote, statement);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private void insertVote(long pollId, PollVote pollVote, PreparedStatement statement) throws SQLException {
        statement.setLong(1, pollId);
        statement.setInt(2, pollVote.multipleChoiceAnswer());
        statement.setString(3, pollVote.userId().toString());
        statement.setString(4, pollVote.choiceAnswer().name());
        statement.setTimestamp(5, pollVote.votedAt());
    }
}
