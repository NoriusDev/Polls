package net.norius.polls.utils;

import net.kyori.adventure.text.Component;
import net.norius.polls.Polls;

import java.sql.Timestamp;

public class DurationFormatter {

    public static Component format(Timestamp timestamp, Polls plugin) {
        long millis = timestamp.getTime() - System.currentTimeMillis();

        long seconds = millis / 1000;

        if(seconds <= 0) {
            return plugin.getConfigLoader().get("gui.duration.ended");
        }

        long weeks = seconds / (7 * 24 * 60 * 60);
        seconds %= (7 * 24 * 60 * 60);

        long days = seconds / (24 * 60 * 60);
        seconds %= (24 * 60 * 60);

        long hours = seconds / (60 * 60);
        seconds %= (60 * 60);

        long minutes = seconds / 60;
        seconds %= 60;

        String path = "gui.duration.";

        Component secs = plugin.getConfigLoader().get(path + "seconds");
        Component mins = plugin.getConfigLoader().get(path + "minutes");
        Component hrs  = plugin.getConfigLoader().get(path + "hours");
        Component dys  = plugin.getConfigLoader().get(path + "days");
        Component wks  = plugin.getConfigLoader().get(path + "weeks");

        Component result;

        if (weeks > 0) {
            result = Component.text(weeks).append(wks);
            if (days > 0) {
                result = result.append(Component.space())
                        .append(Component.text(days)).append(dys);
            }
        } else if (days > 0) {
            result = Component.text(days).append(dys);
            if (hours > 0) {
                result = result.append(Component.space())
                        .append(Component.text(hours)).append(hrs);
            }
        } else if (hours > 0) {
            result = Component.text(hours).append(hrs);
            if (minutes > 0) {
                result = result.append(Component.space())
                        .append(Component.text(minutes)).append(mins);
            }
        } else if (minutes > 0) {
            result = Component.text(minutes).append(mins);
            if (seconds > 0) {
                result = result.append(Component.space())
                        .append(Component.text(seconds)).append(secs);
            }
        } else {
            result = Component.text(seconds).append(secs);
        }

        return result;
    }

}
