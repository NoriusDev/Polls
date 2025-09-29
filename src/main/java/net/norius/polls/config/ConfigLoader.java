package net.norius.polls.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.norius.polls.Polls;

import java.util.List;
import java.util.logging.Level;

public record ConfigLoader(Polls plugin) {

    public ConfigLoader(Polls plugin) {
        this.plugin = plugin;

        plugin.getConfig().options().copyDefaults(true);
        plugin.saveDefaultConfig();
    }

    public Component get(String path) {
        String string = plugin.getConfig().getString("language." + path);

        if (string == null) {
            plugin.getLogger().log(Level.WARNING, "Missing config entry at path: language." + path);
            return Component.empty();
        }

        return formatString(string);
    }

    public Component get(String path, String[] keys, Component[] values) {
        String string = plugin.getConfig().getString("language." + path);

        if (string == null) {
            plugin.getLogger().log(Level.WARNING, "Missing config entry at path: language." + path);
            return Component.empty();
        }

        if (keys.length != values.length) {
            plugin.getLogger().log(Level.WARNING, "Placeholder mismatch at path: language." + path);
            return formatString(string);
        }

        return replace(formatString(string), keys, values);
    }

    public List<Component> getList(String path) {
        List<String> rawList = plugin.getConfig().getStringList("language." + path);

        if (rawList.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "Missing or empty config list at path: language." + path);
            return List.of();
        }

        return rawList.stream().map(this::formatString).toList();
    }

    public List<Component> getList(String path, String[] keys, Component[] values) {
        List<String> rawList = plugin.getConfig().getStringList("language." + path);

        if (rawList.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "Missing or empty config list at path: language." + path);
            return List.of();
        }

        if (keys.length != values.length) {
            plugin.getLogger().log(Level.WARNING, "Placeholder mismatch in list at path: language." + path);
            return rawList.stream().map(this::formatString).toList();
        }

        return rawList.stream()
                .map(s -> replace(formatString(s), keys, values))
                .toList();
    }


    private Component replace(Component text, String[] keys, Component[] values) {
        for (int i = 0; i < keys.length; i++) {
            int finalI = i;
            text = text.replaceText(builder -> builder.match("%" + keys[finalI] + "%").replacement(values[finalI]));
        }

        return text;
    }

    private Component formatString(String s) {
        return MiniMessage.miniMessage().deserialize(s).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
