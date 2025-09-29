package net.norius.polls.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([a-zA-Z]+)");

    public static long parseToMillis(String input) {
        Matcher matcher = TIME_PATTERN.matcher(input.trim().toLowerCase());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time format: " + input);
        }

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        return switch (unit) {
            case "s", "sec", "secs", "second", "seconds" -> TimeUnit.SECONDS.toMillis(value);
            case "m", "min", "mins", "minute", "minutes" -> TimeUnit.MINUTES.toMillis(value);
            case "h", "hr", "hrs", "hour", "hours" -> TimeUnit.HOURS.toMillis(value);
            case "d", "day", "days" -> TimeUnit.DAYS.toMillis(value);
            case "w", "week", "weeks" -> TimeUnit.DAYS.toMillis(value * 7);
            default -> throw new IllegalArgumentException("Unknown time unit: " + unit);
        };
    }
}

