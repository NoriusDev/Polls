package net.norius.polls.gui.menus;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.gui.Menu;
import net.norius.polls.poll.PollSettings;
import net.norius.polls.poll.enums.AnswerType;
import net.norius.polls.utils.ItemBuilder;
import net.norius.polls.utils.SoundUtil;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.Collections;

public class SetChoicesMenu extends Menu {

    private final PollSettings pollSettings;

    public SetChoicesMenu(Polls plugin, PollSettings settings) {
        super(plugin, 27, "gui.set-choice-menu.");
        this.pollSettings = settings;
    }

    @Override
    public void setItems(Inventory inv) {
        setCircle(inv);

        int count = 0;
        for(int i = 10; i < inv.getSize(); i++) {
            if(inv.getItem(i) == null) {
                if(pollSettings.getMultipleChoices().size() <= count) break;
                inv.setItem(i, new ItemBuilder(Material.PAPER)
                        .name(getPlugin().getConfigLoader().get(getPath() + "answer-choice.name",
                                new String[]{"number", "text"},
                                new Component[]{Component.text(count + 1), Component.text(pollSettings.getMultipleChoices().get(count))}))
                        .lore(getPlugin().getConfigLoader().getList(getPath() + "answer-choice.lore"))
                        .build());
                count++;
            } else {
                break;
            }
        }

        inv.setItem(18, new ItemBuilder(Material.SPRUCE_DOOR)
                .name(getPlugin().getConfigLoader().get(getPath() + "back.name"))
                .lore(getPlugin().getConfigLoader().getList(getPath() + "back.lore"))
                .build());

        inv.setItem(26, new ItemBuilder(Material.ANVIL)
                .name(getPlugin().getConfigLoader().get(getPath() + "add.name"))
                .lore(getPlugin().getConfigLoader().getList(getPath() + "add.lore"))
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        switch (event.getSlot()) {
            case 18 -> {
                player.openInventory(new CreatePollMenu(getPlugin(), pollSettings).create());
                SoundUtil.playButtonClick(player);
            }
            case 26 -> {
                if(pollSettings.getMultipleChoices().size() >= 6) {
                    player.sendMessage(getPlugin().getConfigLoader().get("commands.createpoll.max-answers"));
                    SoundUtil.playError(player);
                    return;
                }

                SoundUtil.playButtonClick(player);

                new AnvilGUI.Builder()
                        .plugin(getPlugin())
                        .title(getPlugin().getConfig().getString("language." + getPath() + "add.anvil-title"))
                        .text(getPlugin().getConfig().getString("language." + getPath() + "add.anvil-text"))
                        .onClose(stateSnapshot -> player.openInventory(getInventory()))
                        .onClick((slot, state) -> {
                            if(slot != 2) return Collections.singletonList(AnvilGUI.ResponseAction.run(() -> {}));
                            if(state.getText().isEmpty() || state.getText().isBlank() || pollSettings.getMultipleChoices().contains(state.getText())) {
                                return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(getPlugin().getConfig().getString("language." + getPath() + "add.anvil-invalid")));
                            }

                            pollSettings.getMultipleChoices().add(state.getText());
                            pollSettings.setAnswerType(AnswerType.MULTIPLE_CHOICES);
                            return Collections.singletonList(AnvilGUI.ResponseAction.openInventory(new SetChoicesMenu(getPlugin(), pollSettings).create()));
                        }).open(player);
            }
            default -> {
                if(event.getCurrentItem().getType() != Material.PAPER) return;

                int count = -1;
                for(int i = 10; i < inv.getSize(); i++) {
                    if(inv.getItem(i) == null) break;
                    if(inv.getItem(i).getType() == Material.PAPER) {
                        count++;
                    } else {
                        break;
                    }
                }

                SoundUtil.playButtonClick(player);
                pollSettings.getMultipleChoices().remove(count);

                if(pollSettings.getMultipleChoices().isEmpty())
                    pollSettings.setAnswerType(null);

                player.openInventory(new SetChoicesMenu(getPlugin(), pollSettings).create());
            }
        }
    }
}
