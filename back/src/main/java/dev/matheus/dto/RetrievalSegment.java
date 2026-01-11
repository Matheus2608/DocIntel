package dev.matheus.dto;

import java.util.Objects;

public record RetrievalSegment(
        String question,
        String chunk,
        Double similarity,
        Double modelScore
) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RetrievalSegment that = (RetrievalSegment) o;
        return Objects.equals(chunk, that.chunk);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(chunk);
    }

    @Override
    public String toString() {
        return "Question{" +
                "question='" + question + '\'' +
                ", chunk='" + chunk + '\'' +
                ", similarity=" + similarity +
                ", modelScore=" + modelScore +
                '}';
    }
}
