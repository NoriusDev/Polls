package net.norius.polls.managers;

import lombok.Getter;
import net.norius.polls.Polls;
import net.norius.polls.database.daos.PollLoadingDAO;
import net.norius.polls.database.daos.PollSavingDAO;
import net.norius.polls.poll.Poll;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PollManager {

    @Getter
    private final Map<Long, Poll> polls = new ConcurrentHashMap<>();
    private final Polls plugin;
    @Getter
    private final PollLoadingDAO pollLoadingDAO;
    @Getter
    private final PollSavingDAO pollSavingDAO;

    public PollManager(Polls plugin) {
        this.plugin = plugin;
        this.pollLoadingDAO = new PollLoadingDAO(plugin);
        this.pollSavingDAO = new PollSavingDAO(plugin);
    }

    public void loadActivePolls() {
        pollLoadingDAO.loadActivePolls().thenAcceptAsync(polls::putAll);
    }

    public CompletableFuture<Void> saveActivePolls() {
        return pollSavingDAO.saveActivePolls(polls.entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .collect(Collectors.toSet()));
    }

    public Optional<Poll> getPoll(long id) {
        return Optional.ofNullable(polls.get(id));
    }
}
