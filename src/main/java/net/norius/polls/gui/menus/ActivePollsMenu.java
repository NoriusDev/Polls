package net.norius.polls.gui.menus;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.gui.Menu;
import net.norius.polls.poll.Poll;
import net.norius.polls.utils.ItemBuilder;
import net.norius.polls.utils.LoreUtil;
import net.norius.polls.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ActivePollsMenu extends Menu {

    private final LinkedHashMap<Long, Poll> activePolls;
    private final Player player;

    public ActivePollsMenu(Polls plugin, Player player) {
        super(plugin, 27, "gui.active-polls-menu.");
        this.activePolls = plugin.getPollManager().getSortedPolls(true);
        this.player = player;

        setSize(calculateInvSize(activePolls.size()));
    }

    @Override
    public void setItems(Inventory inv) {
        setCircle(inv);

        int count = 0;
        NamespacedKey key = new NamespacedKey(getPlugin(), "poll_id");
        List<Map.Entry<Long, Poll>> polls = new ArrayList<>(activePolls.entrySet());

        for(int i = 10; i < inv.getSize(); i++) {
            if(inv.getItem(i) != null) continue;
            if(activePolls.size() <= count) break;

            Map.Entry<Long, Poll> entry = polls.get(count);
            Long pollId = entry.getKey();
            Poll poll = entry.getValue();

            inv.setItem(i, new ItemBuilder(Material.PAPER)
                    .name(getPlugin().getConfigLoader().get(getPath() + "poll.name",
                            new String[]{"question"}, new Component[]{Component.text(poll.getQuestion())}))
                    .lore(LoreUtil.buildPollLore(player, getPlugin(), poll, pollId))
                    .data(PersistentDataType.LONG, pollId, key)
                    .build());

            count++;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if(event.getCurrentItem().getType() != Material.PAPER) return;

        long pollId = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(getPlugin(), "poll_id"),
                PersistentDataType.LONG);

        getPlugin().getPollManager().getPoll(pollId).ifPresentOrElse(poll -> {
            if(!poll.isActive()) return;
            if(poll.getPollVotes().stream().anyMatch(pollVote -> pollVote.userId().equals(player.getUniqueId()))) {
                player.sendMessage(getPlugin().getConfigLoader().get(getPath() + "already-voted"));
                SoundUtil.playError(player);
                return;
            }

            player.openInventory(new PollVoteMenu(getPlugin(), poll, pollId, false).create());
            SoundUtil.playButtonClick(player);
        }, () -> player.openInventory(new ActivePollsMenu(getPlugin(), player).create()));
    }
}
