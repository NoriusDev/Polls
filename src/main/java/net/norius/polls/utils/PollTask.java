package net.norius.polls.utils;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.poll.Poll;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Timestamp;
import java.time.Instant;

public class PollTask {

    private final Polls plugin;
    private final NamespacedKey key;

    public PollTask(Polls plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "poll_id");

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () ->
                plugin.getPollManager().getPolls().forEach((key, value) -> {
                    updatePollDurations(value, key);
                    checkEndingTime(value, key);
                    checkDeletion(value, key);
                }), 40, 20);
    }

    private void updatePollDurations(Poll poll, long pollId) {
        plugin.getPollManager().getViewers().forEach(player -> {
            for(ItemStack item : player.getOpenInventory().getTopInventory().getContents()) {
                if(item == null || item.isEmpty() || !item.hasItemMeta()) continue;
                if(!item.getItemMeta().getPersistentDataContainer().has(key)) continue;

                long itemPollId = item.getPersistentDataContainer().get(key, PersistentDataType.LONG);
                if(pollId != itemPollId) continue;

                item.editMeta(itemMeta -> itemMeta.lore(LoreUtil.buildPollLore(player, plugin, poll, pollId)));
                break;
            }
        });
    }

    private void checkEndingTime(Poll poll, long pollId) {
        if(!poll.isActive()) return;

        Timestamp now = Timestamp.from(Instant.now());
        if(poll.getEndingAt().equals(now) || poll.getEndingAt().before(now)) {
            plugin.getPollManager().closeActivePoll(pollId, poll);
        }
    }

    private void checkDeletion(Poll poll, long pollId) {
        if(poll.isActive()) return;

        long deletionMs = plugin.getConfig().getLong("settings.poll-deletion-ms");

        if(System.currentTimeMillis() - poll.getEndingAt().getTime() >= deletionMs) {
            plugin.getPollManager().removePoll(pollId);
        }
    }
}
