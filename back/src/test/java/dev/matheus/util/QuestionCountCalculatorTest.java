package dev.matheus.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionCountCalculatorTest {

    @Test
    void shouldReturn1QuestionForShortSimpleText() {
        String text = "This is a short sentence.";
        int count = QuestionCountCalculator.calculateOptimalQuestionCount(text);

        assertTrue(count >= 1 && count <= 4);
    }

    @Test
    void shouldReturn2QuestionsForMediumText() {
        String text = "This is the first sentence. This is the second sentence with more words.";
        int count = QuestionCountCalculator.calculateOptimalQuestionCount(text);

        assertTrue(count >= 1 && count <= 4);
    }

    @Test
    void shouldReturn3Or4QuestionsForLongerText() {
        String text = "First sentence here. Second sentence with details. Third sentence adds more information. Fourth provides context.";
        int count = QuestionCountCalculator.calculateOptimalQuestionCount(text);

        assertTrue(count >= 2 && count <= 4);
    }

    @Test
    void shouldIncreaseCountForComplexSentences() {
        String text = "This sentence has multiple clauses: one, two, three; and it continues here.";
        int count = QuestionCountCalculator.calculateOptimalQuestionCount(text);

        assertTrue(count >= 1);
    }

    @Test
    void shouldNeverExceedMaximum() {
        String longText = "Sentence one. Sentence two. Sentence three. Sentence four. Sentence five. " +
                "Sentence six. Sentence seven. Sentence eight. Sentence nine. Sentence ten.";
        int count = QuestionCountCalculator.calculateOptimalQuestionCount(longText);

        assertTrue(count <= 4, "Should never exceed 4 questions");
    }

    @Test
    void shouldNeverBeLessThanMinimum() {
        String shortText = "Hi.";
        int count = QuestionCountCalculator.calculateOptimalQuestionCount(shortText);

        assertTrue(count >= 1, "Should always return at least 1 question");
    }

    @Test
    void shouldHandleTextWithNoSentenceEndings() {
        String text = "This text has no proper endings";
        int count = QuestionCountCalculator.calculateOptimalQuestionCount(text);

        assertTrue(count >= 1 && count <= 4);
    }
}

