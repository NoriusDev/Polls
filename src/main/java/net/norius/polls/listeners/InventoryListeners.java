package net.norius.polls.listeners;

import net.norius.polls.Polls;
import net.norius.polls.gui.Menu;
import net.norius.polls.gui.menus.ActivePollsMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public record InventoryListeners(Polls plugin) implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == null) return;
        if(!event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        if(!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;

        event.setCancelled(true);

        if(event.getCurrentItem() == null) return;
        menu.handleClick(event);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if(event.getView().getTopInventory().getHolder() == null) return;
        if(event.getView().getTopInventory().getHolder() instanceof ActivePollsMenu) {
            plugin.getPollManager().getViewers().add((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if(event.getView().getTopInventory().getHolder() == null) return;
        if(event.getView().getTopInventory().getHolder() instanceof ActivePollsMenu) {
            plugin.getPollManager().getViewers().remove((Player) event.getPlayer());
        }
    }
}
