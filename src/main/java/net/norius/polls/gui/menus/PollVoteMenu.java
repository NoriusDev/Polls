package net.norius.polls.gui.menus;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.gui.Menu;
import net.norius.polls.poll.Poll;
import net.norius.polls.poll.PollVote;
import net.norius.polls.poll.enums.AnswerType;
import net.norius.polls.poll.enums.ChoiceAnswer;
import net.norius.polls.utils.ItemBuilder;
import net.norius.polls.utils.LoreUtil;
import net.norius.polls.utils.ResultsCalculator;
import net.norius.polls.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class PollVoteMenu extends Menu {

    private final Poll poll;
    private final long pollId;
    private final boolean showResults;

    public PollVoteMenu(Polls plugin, Poll poll, long pollId, boolean showResults) {
        super(plugin, 45, showResults ? "gui.poll-results-menu." : "gui.poll-vote-menu.");
        this.poll = poll;
        this.pollId = pollId;
        this.showResults = showResults;
    }

    @Override
    public void setItems(Inventory inv) {
        ItemStack glass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();
        for(int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, glass);
        }

        List<Component> lore;

        if(showResults) {
            Component votedAnswer = ResultsCalculator.calculateVotedAnswer(poll, getPlugin());
            lore = votedAnswer == null ? getPlugin().getConfigLoader().getList(getPath() + "question.no-winner-lore") :
                    getPlugin().getConfigLoader().getList(getPath() + "question.lore", new String[]{"answer", "percent"},
                            new Component[]{votedAnswer, Component.text(ResultsCalculator.calculateVotingPercent(poll,
                                    ResultsCalculator.calculateVotedChoiceAnswer(poll),
                                    ResultsCalculator.calculateVotedMultipleChoiceAnswer(poll)))});
        } else {
            lore = getPlugin().getConfigLoader().getList(getPath() + "question.lore");
        }

        inv.setItem(13, new ItemBuilder(Material.WRITABLE_BOOK)
                .name(getPlugin().getConfigLoader().get(getPath() + "question.name",
                        new String[]{"question"}, new Component[]{Component.text(poll.getQuestion())}))
                .lore(lore)
                .build());

        inv.setItem(36, new ItemBuilder(Material.SPRUCE_DOOR)
                .name(getPlugin().getConfigLoader().get("gui.poll-vote-menu.back.name"))
                .lore(getPlugin().getConfigLoader().getList("gui.poll-vote-menu.back.lore"))
                .build());

        if(poll.getAnswerType() == AnswerType.YES_NO) {
            inv.setItem(29, createVoteItem(Material.GREEN_CONCRETE, ChoiceAnswer.YES));
            inv.setItem(33, createVoteItem(Material.RED_CONCRETE, ChoiceAnswer.NO));
        } else {
            switch (poll.getMultipleChoices().size()) {
                case 1 -> inv.setItem(31, createVoteItem(0));
                case 2 -> {
                    inv.setItem(29, createVoteItem(0));
                    inv.setItem(33, createVoteItem(1));
                }
                case 3 -> {
                    inv.setItem(29, createVoteItem(0));
                    inv.setItem(31, createVoteItem(1));
                    inv.setItem(33, createVoteItem(2));
                }
                case 4 -> {
                    for(int i = 0; i < 4; i++) {
                        inv.setItem(29 + i, createVoteItem(i));
                    }

                    inv.setItem(33, null);
                }
                case 5 -> {
                    for(int i = 0; i < 5; i++) {
                        inv.setItem(29 + i, createVoteItem(i));
                    }
                }
                case 6 -> {
                    for(int i = 0; i < 6; i++) {
                        inv.setItem(28 + i, createVoteItem(i));
                    }

                    inv.setItem(34, null);
                }
            }
        }
    }

    private ItemStack createVoteItem(Material material, ChoiceAnswer answer) {
        Component name = showResults ? getPlugin().getConfigLoader().get(getPath() + "result.name", new String[]{"answer", "percent"},
                new Component[]{
                        getPlugin().getConfigLoader().get("gui.poll-vote-menu.vote-" + answer.name().toLowerCase() + ".name"),
                        Component.text(ResultsCalculator.calculateVotingPercent(poll, answer, 0))
                })
                : getPlugin().getConfigLoader().get(getPath() + "vote-" + answer.name().toLowerCase() + ".name");

        return new ItemBuilder(material)
                .name(name)
                .lore(LoreUtil.buildResultsLore(showResults, getPlugin(), poll, getPath(), 0, answer))
                .build();
    }

    private ItemStack createVoteItem(int index) {
        return new ItemBuilder(Material.LIME_DYE)
                .name(showResults ? getPlugin().getConfigLoader().get(getPath() + "result.name", new String[]{"answer", "percent"},
                        new Component[]{
                                Component.text(poll.getMultipleChoices().get(index)),
                                Component.text(ResultsCalculator.calculateVotingPercent(poll, null, index))}) :
                        getPlugin().getConfigLoader().get(getPath() + "multiple-choice.name",
                                new String[]{"number", "answer"}, new Component[]{Component.text(index + 1), Component.text(poll.getMultipleChoices().get(index))}))
                .lore(LoreUtil.buildResultsLore(showResults, getPlugin(), poll, getPath(), index, null))
                .data(PersistentDataType.INTEGER, index, new NamespacedKey(getPlugin(), "choice_id"))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if(this.poll == null) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        switch (item.getType()) {
            case SPRUCE_DOOR -> {
                SoundUtil.playButtonClick(player);
                player.openInventory(showResults ? new EndedPollsMenu(getPlugin()).create() : new ActivePollsMenu(getPlugin(), player).create());
            }
            case GREEN_CONCRETE, RED_CONCRETE, LIME_DYE -> {
                if(showResults || !poll.isActive()) return;
                SoundUtil.playSuccess(player);
                PollVote pollVote;

                boolean isLime = item.getType() == Material.LIME_DYE;
                boolean isYes = item.getType() == Material.GREEN_CONCRETE;
                int choiceId = 0;

                if(isLime) {
                    choiceId = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(getPlugin(), "choice_id"), PersistentDataType.INTEGER);
                    pollVote = new PollVote(
                            player.getUniqueId(),
                            null,
                            Timestamp.from(Instant.now()),
                            choiceId
                    );
                } else {
                    pollVote = new PollVote(
                            player.getUniqueId(),
                            isYes ? ChoiceAnswer.YES : ChoiceAnswer.NO,
                            Timestamp.from(Instant.now()),
                            0
                    );
                }

                poll.getPollVotes().add(pollVote);
                getPlugin().getPollManager().savePollVote(pollId, pollVote);
                player.openInventory(new ActivePollsMenu(getPlugin(), player).create());

                player.sendMessage(getPlugin().getConfigLoader().get(getPath() + "voted",
                        new String[]{"answer"}, isLime ? new Component[]{Component.text(poll.getMultipleChoices().get(choiceId))} :
                                new Component[]{getPlugin().getConfigLoader().get(getPath() + "vote-" + pollVote.choiceAnswer().name().toLowerCase() + ".name")}));
            }
        }

    }
}
