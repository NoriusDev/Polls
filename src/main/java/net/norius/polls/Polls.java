package net.norius.polls;

import lombok.Getter;
import net.norius.polls.commands.CreatePollCommand;
import net.norius.polls.commands.EndedPollsCommand;
import net.norius.polls.commands.PollCommand;
import net.norius.polls.config.ConfigLoader;
import net.norius.polls.database.Database;
import net.norius.polls.listeners.InventoryListeners;
import net.norius.polls.managers.PollManager;
import net.norius.polls.utils.PollTask;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

@Getter
public final class Polls extends JavaPlugin {

    private Database database;
    private PollManager pollManager;
    private ConfigLoader configLoader;

    private boolean isDisabling;

    @Override
    public void onEnable() {
        configLoader = new ConfigLoader(this);
        database = new Database(this);

        if(isDisabling) return;

        pollManager = new PollManager(this);

        database.createTables()
                .thenRun(() -> pollManager.loadLastPollId())
                .thenRun(() -> pollManager.loadActivePolls());

        new PollTask(this);

        register();
    }

    @Override
    public void onDisable() {
        isDisabling = true;
        if(database != null && !database.getExecutor().isShutdown()) {
            pollManager.saveActivePolls().thenRun(() -> database.getExecutor().shutdownNow());
        }
    }

    private void register() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new InventoryListeners(this), this);

        Objects.requireNonNull(getCommand("poll")).setExecutor(new PollCommand(this));
        Objects.requireNonNull(getCommand("createpoll")).setExecutor(new CreatePollCommand(this));
        Objects.requireNonNull(getCommand("endedpolls")).setExecutor(new EndedPollsCommand(this));
    }

}
