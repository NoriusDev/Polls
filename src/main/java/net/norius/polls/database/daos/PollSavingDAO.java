package net.norius.polls.database.daos;

import lombok.RequiredArgsConstructor;
import net.norius.polls.Polls;
import net.norius.polls.poll.Poll;
import net.norius.polls.poll.PollVote;

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

                    connection.commit();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save active polls", e);
            }
        }, plugin.getDatabase().getExecutor());
    }

    public CompletableFuture<Void> savePoll(long pollId, Poll poll) {
        return CompletableFuture.runAsync(() -> {
            try(Connection connection = plugin.getDatabase().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO polls_polls(poll_id, question_text, answer_type, created_at, poll_duration, is_active, poll_creator) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE " +
                                "poll_id = VALUES(poll_id), " +
                                "question_text = VALUES(question_text), " +
                                "answer_type = VALUES(answer_type), " +
                                "created_at = VALUES(created_at), " +
                                "poll_duration = VALUES(poll_duration), " +
                                "is_active = VALUES(is_active), " +
                                "poll_creator = VALUES(poll_creator)")) {

                connection.setAutoCommit(false);
                statement.setLong(1, pollId);
                statement.setString(2, poll.getQuestion());
                statement.setString(3, poll.getAnswerType().name());
                statement.setTimestamp(4, poll.getCreatedAt());
                statement.setLong(5, poll.getEndingAt().getTime() - System.currentTimeMillis());
                statement.setBoolean(6, poll.isActive());
                statement.setString(7, poll.getCreator().toString());
                statement.executeUpdate();

                saveVotes(pollId, poll, connection);

                connection.commit();

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save poll", e);
            }
        }, plugin.getDatabase().getExecutor());
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
        statement.setString(4, pollVote.userId().toString());
        statement.setString(5, pollVote.choiceAnswer().name());
        statement.setTimestamp(6, pollVote.votedAt());
    }
}
