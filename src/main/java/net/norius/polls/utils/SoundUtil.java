package net.norius.polls.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    public static void playButtonClick(Player player) {
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
    }

    public static void playSuccess(Player player) {
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    }

    public static void playError(Player player) {
        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
    }
}
