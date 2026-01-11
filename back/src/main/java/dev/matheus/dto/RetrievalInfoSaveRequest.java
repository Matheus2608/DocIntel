package dev.matheus.dto;

import java.util.List;


public record RetrievalInfoSaveRequest(
        String chatMessageId,
        String userQuestion,
        List<RetrievalSegment> retrievalSegments
) {
    @Override
    public String toString() {
        return "RetrievalInfoSaveRequest{" +
                "chatMessageId='" + chatMessageId + '\'' +
                ", userQuestion='" + userQuestion + '\'' +
                ", hypoteticalQuestions=" + retrievalSegments +
                '}';
    }
}

