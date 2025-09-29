package net.norius.polls.poll;

import lombok.Data;
import net.norius.polls.poll.enums.AnswerType;

import java.util.List;

@Data
public class PollSettings {

    private final String question;
    private final long duration;
    private final List<String> multipleChoices;
    private AnswerType answerType;
}
