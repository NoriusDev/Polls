package net.norius.polls.gui.menus;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.gui.Menu;
import net.norius.polls.poll.PollSettings;
import net.norius.polls.poll.enums.AnswerType;
import net.norius.polls.utils.DurationFormatter;
import net.norius.polls.utils.ItemBuilder;
import net.norius.polls.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CreatePollMenu extends Menu {

    private final PollSettings pollSettings;

    public CreatePollMenu(Polls plugin, PollSettings pollSettings) {
        super(plugin, InventoryType.HOPPER, "gui.create-poll-menu.");
        this.pollSettings = pollSettings;
    }

    @Override
    public void setItems(Inventory inv) {
        ItemStack blackGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();

        inv.setItem(1, blackGlass);
        inv.setItem(3, blackGlass);

        setYesNo(inv);
        setConfirmation(inv);
        setMultipleAnswers(inv);
    }

    private void setYesNo(Inventory inv) {
        List<Component> lore = new ArrayList<>(getPlugin().getConfigLoader().getList(getPath() + "yes-no.lore"));

        if(pollSettings.getAnswerType() == AnswerType.YES_NO) {
            lore.addAll(getPlugin().getConfigLoader().getList(getPath() + "selected-lore"));
        }

        inv.setItem(0, new ItemBuilder(Material.LIME_DYE)
                .name(getPlugin().getConfigLoader().get(getPath() + "yes-no.name"))
                .lore(lore)
                .build());
    }

    private void setConfirmation(Inventory inv) {
        inv.setItem(2, new ItemBuilder(Material.GREEN_CONCRETE)
                .name(getPlugin().getConfigLoader().get(getPath() + "confirm.name"))
                .lore(getPlugin().getConfigLoader().getList(getPath() + "confirm.lore",
                        new String[]{"question", "duration", "answer-type"},
                        new Component[]{
                                Component.text(pollSettings.getQuestion()),
                                DurationFormatter.format(new Timestamp(System.currentTimeMillis() + pollSettings.getDuration()), getPlugin()),
                                pollSettings.getAnswerType() == null ? getPlugin().getConfigLoader().get(getPath() + "not-selected") :
                                        getPlugin().getConfigLoader().get(getPath() + pollSettings.getAnswerType().name().toLowerCase().replace('_', '-') + ".name")}))
                .build());
    }

    private void setMultipleAnswers(Inventory inv) {
        List<Component> lore = new ArrayList<>(getPlugin().getConfigLoader().getList(getPath() + "multiple-choices.lore"));

        if(pollSettings.getAnswerType() == AnswerType.MULTIPLE_CHOICES) {
            lore.addAll(getPlugin().getConfigLoader().getList(getPath() + "selected-lore"));
        }

        inv.setItem(4, new ItemBuilder(Material.WRITABLE_BOOK)
                .name(getPlugin().getConfigLoader().get(getPath() + "multiple-choices.name"))
                .lore(lore)
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 0 -> {
                if(pollSettings.getAnswerType() == AnswerType.YES_NO) return;
                pollSettings.setAnswerType(AnswerType.YES_NO);

                setConfirmation(inv);
                setYesNo(inv);
                SoundUtil.playButtonClick(player);
            }
            case 2 -> {
                if(pollSettings.getAnswerType() == null) {
                    player.sendMessage(getPlugin().getConfigLoader().get("commands.createpoll.no-answer-type"));
                    SoundUtil.playError(player);
                    return;
                }

                getPlugin().getPollManager().createPoll(player.getUniqueId(), pollSettings);
                player.sendMessage(getPlugin().getConfigLoader().get("commands.createpoll.created"));
                SoundUtil.playSuccess(player);
                player.closeInventory();

                if(!getPlugin().getConfig().getBoolean("settings.broadcast-on-poll-creation")) return;

                Component message = getPlugin().getConfigLoader().get("broadcast.creation",
                        new String[]{"question"}, new Component[]{Component.text(pollSettings.getQuestion())});

                Bukkit.getOnlinePlayers().forEach(online -> online.sendMessage(message));
            }
            case 4 -> {
                player.openInventory(new SetChoicesMenu(getPlugin(), pollSettings).create());
                SoundUtil.playButtonClick(player);
            }
        }
    }
}
