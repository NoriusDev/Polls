package net.norius.polls.poll;

public enum PollDuration {

    MINUTES(1000),
    HOURS(),
    DAYS,
    WEEKS;

    PollDuration(long duration, String... aliases) {

    }
}
