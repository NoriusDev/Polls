package net.norius.polls.poll;

import java.sql.Timestamp;
import java.util.UUID;

public record PollVote(UUID userId, ChoiceAnswer choiceAnswer, Timestamp votedAt, int multipleChoiceAnswer) {

}
