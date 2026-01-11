package dev.matheus.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@RegisterAiService(modelName = "fake-answer-generator")
@ApplicationScoped
public interface FakeAnswerAiService {

    @SystemMessage("""
        Gere respostas falsas detalhadas para as seguintes perguntas
        As respostas devem ser plausíveis, mas não verdadeiras.
        Use um tom informativo e formal.
        Retorne um array JSON de strings com as respostas falsas.
        """)
    String fakeAnswer(@UserMessage String text);

}
