package net.norius.polls.poll;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Poll {

    private final String question;
    private final AnswerType answerType;
    private final List<String> multipleChoices;
    private final List<PollVote> pollVotes;
    private final Timestamp createdAt;
    private final Timestamp endingAt;
    private boolean isActive;
    private final UUID creator;
}
