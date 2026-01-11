package dev.matheus;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiScoringModel;
import dev.matheus.splitter.SmartDocumentSplitter;
import dev.matheus.splitter.TwoToThreeSentenceSplitter;
import dev.matheus.util.ChatUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReRankTest {

    @Test
    void testIfApiWorks() {
        VertexAiScoringModel scoringModel = VertexAiScoringModel.builder()
                .projectId(System.getenv("GCP_PROJECT_ID"))
                .projectNumber(System.getenv("GCP_PROJECT_NUM"))
//                .location("us-central1")
                .location("southamerica-east1")
                .model("semantic-ranker-512")
                .build();

        Response<List<Double>> score = scoringModel.scoreAll(Stream.of(
                        "The sky appears blue due to a phenomenon called Rayleigh scattering. " +
                                "Sunlight is comprised of all the colors of the rainbow. Blue light has shorter " +
                                "wavelengths than other colors, and is thus scattered more easily.",

                        "A canvas stretched across the day,\n" +
                                "Where sunlight learns to dance and play.\n" +
                                "Blue, a hue of scattered light,\n" +
                                "A gentle whisper, soft and bright."
                ).map(TextSegment::from).collect(Collectors.toList()),
                "Why is the sky blue?");

        score.content().forEach(
                System.out::println
        );

// [0.8199999928474426, 0.4300000071525574]
    }


    @Test
    void testGemini() {
        ChatModel gemini = GoogleAiGeminiChatModel.builder()
                .apiKey("AIzaSyCdWH2-lURtUuZDoOt-wnJ5eW5ePD1Yi2Q")
                .httpClientBuilder(new JdkHttpClientBuilder())
                .modelName("gemini-2.5-flash")
                .build();

        ChatResponse chatResponse = gemini.chat(ChatRequest.builder()
                .messages(UserMessage.from(
                        "How many R's are there in the word 'strawberry'?"))
                .build());

        String response = chatResponse.aiMessage().text();

        System.out.println(response);
    }

    private void splitAndGenerateChuncks(String fileName, DocumentParser parser, DocumentSplitter splitter) {
        String path = getClass().getClassLoader().getResource(fileName).getPath();
        var document = FileSystemDocumentLoader.loadDocument(path, parser);

        List<TextSegment> paragraphs = splitter.split(document);

        int totalNumerOfQuestions = 0;
        for (TextSegment paragraphSegment : paragraphs) {
            yellowPrint("---- Chunck ----");
            greenPrint(paragraphSegment.text());
            yellowPrint("---- Number of questions ----");
            totalNumerOfQuestions += ChatUtils.getNumberQuestions(paragraphSegment.text());
            greenPrint(String.valueOf(ChatUtils.getNumberQuestions(paragraphSegment.text())));
        }

        yellowPrint("---- Total Number of questions ----");
        greenPrint(String.valueOf(totalNumerOfQuestions));
    }

    @Test
    void testGenerateSplittingAndNumberQuestions() {
        String fileName = "files/Manual.txt";
        String path = getClass().getClassLoader().getResource(fileName).getPath();

        var document = FileSystemDocumentLoader
                .loadDocument(path, new TextDocumentParser());

        TwoToThreeSentenceSplitter splitter = new TwoToThreeSentenceSplitter();
        List<TextSegment> paragraphs = splitter.split(document);

        for (TextSegment paragraphSegment : paragraphs) {
            yellowPrint("---- Chunck ----");
            greenPrint(paragraphSegment.text());
            yellowPrint("---- Number of questions ----");
            greenPrint(String.valueOf(ChatUtils.getNumberQuestions(paragraphSegment.text())));
        }
    }

    @Test
    void testGenerateSplittingAndNumberQuestions2() {
        String fileName = "files/GuiaDoAtleta2025.pdf";
        var pdfParser = new ApachePdfBoxDocumentParser();
        DocumentSplitter splitter = new SmartDocumentSplitter();
//        splitAndGenerateChuncks(fileName, pdfParser, splitter);

        greenPrint(" -------------- Using TwoToThreeSentenceSplitter -------------- ");
        splitter = new TwoToThreeSentenceSplitter();
        splitAndGenerateChuncks(fileName, pdfParser, splitter);
    }



    private void yellowPrint(String text) {
        System.out.println("\u001B[33m" + text + "\u001B[0m");
    }

    private void greenPrint(String text) {
        System.out.println("\u001B[32m" + text + "\u001B[0m");
    }
}
