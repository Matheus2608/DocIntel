package dev.matheus.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface LanguageDetectionAiService {

    @SystemMessage("Return only the ISO 639-1 language code (e.g. 'en', 'pt', 'es') for the following text. Return nothing else.")
    String detectLanguage(@UserMessage String textSample);
}
