package net.norius.polls.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import net.norius.polls.Polls;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@Getter
public class Database {

    private HikariDataSource dataSource;
    private ExecutorService executor;
    private final Polls plugin;

    public Database(Polls plugin) {
        this.plugin = plugin;

        HikariConfig config = new HikariConfig();

        String type = plugin.getConfigLoader().getString("database.type");
        String host = plugin.getConfigLoader().getString("database.host");
        String port = plugin.getConfigLoader().getString("database.port");
        String database = plugin.getConfigLoader().getString("database.database");
        String user = plugin.getConfigLoader().getString("database.user");

        if(type.isEmpty() || host.isEmpty() || port.isEmpty() || database.isEmpty() || user.isEmpty()) {
            plugin.getLogger().warning("Database type and host or port are mandatory");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        config.setJdbcUrl(String.format("jdbc:%s://%s:%s/%s",  type, host, port, database));
        config.setUsername(plugin.getConfigLoader().getString("database.username"));
        config.setPassword(plugin.getConfigLoader().getString("database.password"));

        dataSource = new HikariDataSource(config);
        executor = Executors.newCachedThreadPool();
    }

    public CompletableFuture<Void> createTables() {
        return CompletableFuture.runAsync(() -> {
            try(Connection connection = getConnection();
                PreparedStatement polls = connection.prepareStatement("CREATE TABLE IF NOT EXISTS polls_polls(" +
                        "poll_id LONG NOT NULL PRIMARY KEY, " +
                        "question_text VARCHAR(255) NOT NULL, " +
                        "answer_type VARCHAR(20) NOT NULL CHECK (poll_type IN ('YES_NO', 'MULTIPLE_CHOICE')), " +
                        "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                        "poll_duration LONG NOT NULL, " +
                        "is_active BOOLEAN, " +
                        "poll_creator VARCHAR(36) NOT NULL)");
                PreparedStatement votes = connection.prepareStatement("CREATE TABLE IF NOT EXISTS polls_votes(" +
                        "poll_id LONG NOT NULL, " +
                        "choice_order INT, " +
                        "user_id VARCHAR(36) NOT NULL), " +
                        "choice_answer VARCHAR(3) CHECK (choice_answer IN ('YES', 'NO'))," +
                        "voted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (poll_id) REFERENCES polls_polls(poll_id) ON DELETE CASCADE, " +
                        "UNIQUE (poll_id, user_id))");
                PreparedStatement choices = connection.prepareStatement("CREATE TABLE IF NOT EXISTS polls_choices(" +
                        "poll_id LONG NOT NULL, " +
                        "choice_order INT NOT NULL, " +
                        "choice_text VARCHAR(100) NOT NULL, " +
                        "FOREIGN KEY (poll_id) REFERENCES polls_polls(poll_id) ON DELETE CASCADE, " +
                        "UNIQUE (poll_id, choice_order)")) {

                polls.execute();
                votes.execute();
                choices.execute();

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to create database tables!", e);
            }
        }, executor);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
