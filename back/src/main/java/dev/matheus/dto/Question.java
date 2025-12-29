package dev.matheus.dto;

public record Question(
        String question,
        String chunk,
        Double similarity
) {
    @Override
    public String toString() {
        return "Question{" +
                "question='" + question + '\'' +
                ", chunk='" + chunk + '\'' +
                ", similarity=" + similarity +
                '}';
    }
}
