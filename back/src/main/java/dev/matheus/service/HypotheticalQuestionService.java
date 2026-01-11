package dev.matheus.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.matheus.ai.QuestionExtractorAiService;
import dev.matheus.splitter.CustomTableAwareSplitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static dev.matheus.util.ChatUtils.isPdf;
import static dev.matheus.util.ChatUtils.toDocument;

@ApplicationScoped
public class HypotheticalQuestionService {

    private static final Logger Log = Logger.getLogger(HypotheticalQuestionService.class);
    private static final String FILE_NAME_KEY = "FILE_NAME";
    private static final String PARAGRAPH_KEY = "PARAGRAPH";

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    DocumentIngestionService documentIngestionService;

    @Inject
    CustomTableAwareSplitter customTableAwareSplitter;

    @Inject
    QuestionExtractorAiService questionExtractorAiService;

    @Inject
    @Named("retrievalExecutorService")
    ExecutorService executorService;


    public void ingestHypotheticalQuestions(byte[] docBytes, String fileName, String fileType) throws IOException {
        Log.infof("Ingesting hypothetical questions for document: %s", fileName);

        Document document;
        if (isPdf(fileType, fileName)) {
            document = documentIngestionService.parseCustomPdf(docBytes, fileName);
        } else {
            document = toDocument(docBytes, fileType, fileName);
        }

        var paragraphs = customTableAwareSplitter.split(document);

        List<TextSegment> embeddedSegments = getHypotheticalQuestionsAndParagraphsSegments(paragraphs, fileName);
        List<Embedding> embeddings = embeddingModel.embedAll(embeddedSegments).content();
        embeddingStore.addAll(embeddings, embeddedSegments);

        Log.infof("Successfully ingested %d segments for document %s", embeddedSegments.size(), fileName);
    }

    private List<TextSegment> getHypotheticalQuestionsAndParagraphsSegments(List<TextSegment> paragraphs, String fileName) {
        paragraphs.forEach(p -> p.metadata().put(FILE_NAME_KEY, fileName));

        List<QuestionParagraph> questionParagraphs = parallelProcessing(paragraphs, fileName);
        List<TextSegment> embeddedSegments = questionParagraphsToTextSegments(questionParagraphs, fileName);
        Log.infof("Successfully generated %d hypothetical questions for document %s", questionParagraphs.size(), fileName);

        embeddedSegments.addAll(paragraphs); // Also index the original paragraphs
        return embeddedSegments;
    }

    private List<QuestionParagraph> parallelProcessing(List<TextSegment> paragraphs, String fileName) {
        Log.infof("Starting parallel question generation - paragraphs=%d, fileName=%s",
                paragraphs.size(), fileName);

        List<CompletableFuture<List<QuestionParagraph>>> futures = paragraphs.stream()
                .map(paragraphSegment -> CompletableFuture.supplyAsync(() -> {
                    String text = paragraphSegment.text();
                    List<String> questions = text.startsWith("[START_TABLE]")
                            ? questionExtractorAiService.extractQuestionsFromTable(text)
                            : questionExtractorAiService.extractQuestions(text);

                    Log.debugf("Generated %d questions from paragraph", questions.size());

                    List<QuestionParagraph> paragraphQuestions = new ArrayList<>();
                    for (String question : questions) {
                        paragraphQuestions.add(new QuestionParagraph(question, paragraphSegment));
                    }

                    return paragraphQuestions;
                }, executorService))
                .toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        List<QuestionParagraph> result = new ArrayList<>();
        try {
            allOf.join();
            for (CompletableFuture<List<QuestionParagraph>> future : futures) {
                result.addAll(future.join());
            }
            Log.infof("Parallel processing completed - generated %d questions from %d paragraphs for fileName=%s",
                    result.size(), paragraphs.size(), fileName);
        } catch (Exception e) {
            Log.errorf(e, "Error during parallel question generation - fileName=%s, paragraphs=%d",
                    fileName, paragraphs.size());
            throw new RuntimeException("Failed to generate hypothetical questions", e);
        }

        return result;
    }

    private List<TextSegment> questionParagraphsToTextSegments(List<QuestionParagraph> questionParagraphs, String fileName) {
        return questionParagraphs.stream()
                .map(questionParagraph -> TextSegment.from(
                        questionParagraph.question(),
                        new Metadata()
                                .put(PARAGRAPH_KEY, questionParagraph.paragraph().text())
                                .put(FILE_NAME_KEY, fileName)
                ))
                .collect(Collectors.toList());
    }

    private record QuestionParagraph(String question, TextSegment paragraph) {
    }
}
