package net.norius.polls.gui;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;
import net.norius.polls.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public abstract class Menu implements InventoryHolder {

    @Getter
    private final Polls plugin;
    @Getter
    private final String path;

    @Setter
    private int size;
    private InventoryType inventoryType;
    private Inventory inventory;

    public Menu(Polls plugin, int size, String path) {
        this.plugin = plugin;
        this.size = size;
        this.path = path;
    }

    public Menu(Polls plugin, InventoryType inventoryType, String path) {
        this.plugin = plugin;
        this.inventoryType = inventoryType;
        this.path = path;
    }

    public abstract void setItems(Inventory inv);

    public abstract void handleClick(InventoryClickEvent event);

    protected void setCircle(Inventory inv) {
        ItemStack glass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();

        for(int i = 0; i < inv.getSize(); i++) {
            if(i < 9 || i > inv.getSize() - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, glass);
            }
        }
    }

    protected int calculateInvSize(int amount) {
        if(amount == 0)
            return 27;
        else
            return amount > 36 ? 54 : 18 + 9 * (int) Math.ceil((double) amount / 9);
    }

    public Inventory create() {
        Inventory inventory;
        Component title = plugin.getConfigLoader().get(path + "title");

        if(inventoryType == null) {
            inventory = Bukkit.createInventory(this, size, title);
        } else {
            inventory = Bukkit.createInventory(this, inventoryType, title);
        }

        setItems(inventory);
        this.inventory = inventory;

        return inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
