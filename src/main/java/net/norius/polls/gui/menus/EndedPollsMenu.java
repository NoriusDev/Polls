package net.norius.polls.gui.menus;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.gui.Menu;
import net.norius.polls.poll.Poll;
import net.norius.polls.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.*;

public class EndedPollsMenu extends Menu {

    private final LinkedHashMap<Long, Poll> inActivePolls;

    public EndedPollsMenu(Polls plugin) {
        super(plugin, 0, "gui.ended-polls-menu.");
        this.inActivePolls = plugin.getPollManager().getSortedPolls(false);

        setSize(calculateInvSize(inActivePolls.size()));
    }

    @Override
    public void setItems(Inventory inv) {
        setCircle(inv);

        int count = 0;
        List<Map.Entry<Long, Poll>> entries = new ArrayList<>(inActivePolls.entrySet());

        for(int i = 10; i < inv.getSize() - 9; i++) {
            if(inv.getItem(i) != null) continue;
            if(inActivePolls.size() == count) break;

            Poll poll = entries.get(count).getValue();
            long pollId = entries.get(count).getKey();

            inv.setItem(i, new ItemBuilder(Material.PAPER)
                    .name(getPlugin().getConfigLoader().get(getPath() + "poll.name",
                            new String[]{"question"}, new Component[]{Component.text(poll.getQuestion())}))
                    .lore(getPlugin().getConfigLoader().getList(getPath() + "poll.lore",
                            new String[]{"poll-id", "ending-date", "creator"},
                            new Component[]{
                                    Component.text(pollId),
                                    Component.text(new SimpleDateFormat(Objects.requireNonNull(getPlugin().getConfig().getString("language.gui.date-format"))).format(poll.getEndingAt())),
                                    Component.text(Objects.requireNonNull(Bukkit.getOfflinePlayer(poll.getCreator()).getName()))
                            }))
                    .data(PersistentDataType.LONG, pollId, new NamespacedKey(getPlugin(), "poll_id"))
                    .build());

            count++;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if(item.getType() != Material.PAPER) return;

        long pollId = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "poll_id"), PersistentDataType.LONG);

        getPlugin().getPollManager().getPoll(pollId).ifPresentOrElse(poll ->
                player.openInventory(new PollVoteMenu(getPlugin(), poll, pollId, true).create()), () ->
                player.openInventory(new EndedPollsMenu(getPlugin()).create()));
    }
}
