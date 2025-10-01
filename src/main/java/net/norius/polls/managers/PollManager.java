package net.norius.polls.managers;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.database.daos.PollLoadingDAO;
import net.norius.polls.database.daos.PollSavingDAO;
import net.norius.polls.poll.Poll;
import net.norius.polls.poll.PollSettings;
import net.norius.polls.poll.PollVote;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class PollManager {

    @Getter
    private final Map<Long, Poll> polls = new ConcurrentHashMap<>();
    @Getter
    private final Set<Player> viewers = Collections.synchronizedSet(new HashSet<>());

    private final PollLoadingDAO pollLoadingDAO;
    private final PollSavingDAO pollSavingDAO;
    private final AtomicLong lastPollId = new AtomicLong();
    private final Polls plugin;

    public PollManager(Polls plugin) {
        this.pollLoadingDAO = new PollLoadingDAO(plugin);
        this.pollSavingDAO = new PollSavingDAO(plugin);
        this.plugin = plugin;
    }

    public CompletableFuture<Void> loadActivePolls() {
        return pollLoadingDAO.loadPolls().thenAcceptAsync(polls::putAll);
    }

    public CompletableFuture<Void> saveActivePolls() {
        return pollSavingDAO.saveActivePolls(polls.entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .collect(Collectors.toSet()));
    }

    public void savePollVote(long pollId, PollVote pollVote) {
        pollSavingDAO.saveVote(pollId, pollVote);
    }

    public Optional<Poll> getPoll(long id) {
        return Optional.ofNullable(polls.get(id));
    }

    public CompletableFuture<Void> loadLastPollId() {
        return pollLoadingDAO.loadLastPollId().thenAcceptAsync(this.lastPollId::set);
    }

    public LinkedHashMap<Long, Poll> getSortedPolls(boolean active) {
        return polls.entrySet().stream()
                .filter(entry -> {
                    if(active) {
                        return entry.getValue().isActive();
                    } else {
                        return !entry.getValue().isActive();
                    }
                })
                .sorted(Comparator.comparingLong(e -> e.getValue().getEndingAt().getTime()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public void createPoll(UUID creator, PollSettings settings) {
        Poll poll = new Poll(
                settings.getQuestion(),
                settings.getAnswerType(),
                settings.getMultipleChoices(),
                new ArrayList<>(),
                Timestamp.from(Instant.now()),
                new Timestamp(settings.getDuration() + System.currentTimeMillis()),
                true,
                creator
        );

        long pollId = this.lastPollId.incrementAndGet();

        polls.put(pollId, poll);
        pollSavingDAO.savePoll(pollId, poll);
        pollSavingDAO.saveLastPollId(pollId);
    }

    public void closeActivePoll(long pollId, Poll poll) {
        if(!poll.isActive()) return;

        if(plugin.getConfig().getBoolean("settings.broadcast-on-poll-ending")) {
            Component message = plugin.getConfigLoader().get("broadcast.ended", new String[]{"question"}, new Component[]{Component.text(poll.getQuestion())});
            Bukkit.getOnlinePlayers().forEach(online -> online.sendMessage(message));
        }

        poll.setEndingAt(Timestamp.from(Instant.now()));
        poll.setActive(false);
        pollSavingDAO.savePoll(pollId, poll);
    }

    public void removePoll(long pollId) {
        polls.remove(pollId);
        pollSavingDAO.removePoll(pollId);
    }
}
