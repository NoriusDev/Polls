package net.norius.polls;

import lombok.Getter;
import net.norius.polls.config.ConfigLoader;
import net.norius.polls.database.Database;
import net.norius.polls.managers.PollManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Polls extends JavaPlugin {

    private Database database;
    private PollManager pollManager;
    private ConfigLoader configLoader;

    @Override
    public void onEnable() {
        configLoader = new ConfigLoader(this);
        database = new Database(this);
        pollManager = new PollManager(this);

        database.createTables().thenRun(() -> pollManager.loadActivePolls());

        register();
    }

    @Override
    public void onDisable() {
        if(database != null && !database.getExecutor().isShutdown()) {
            pollManager.saveActivePolls().thenRun(() -> database.getExecutor().shutdownNow());
        }
    }

    private void register() {
        PluginManager pm = getServer().getPluginManager();
    }

}
