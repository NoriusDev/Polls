package net.norius.polls.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.norius.polls.Polls;

import java.util.List;

public record ConfigLoader(Polls plugin) {

    public ConfigLoader(Polls plugin) {
        this.plugin = plugin;

        plugin.getConfig().options().copyDefaults(true);
        plugin.saveDefaultConfig();
    }

    public Component get(String path) {
        return formatString(plugin.getConfig().getString("language." + path));
    }

    public Component get(String path, String[] keys, String[] values) {
        return replace(plugin.getConfig().getString("language." + path), keys, values);
    }

    public List<Component> getList(String path) {
        return plugin.getConfig().getStringList("language." + path).stream().map(this::formatString).toList();
    }

    public List<Component> getList(String path, String[] keys, String[] values) {
        return plugin.getConfig().getStringList("language." + path).stream().map(s -> replace(s, keys, values)).toList();
    }

    private Component replace(String text, String[] keys, String[] values) {
        for (int i = 0; i < keys.length; i++) {
            text = text.replace(keys[i], values[i]);
        }

        return formatString(text);
    }

    private Component formatString(String s) {
        return MiniMessage.miniMessage().deserialize(s).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
