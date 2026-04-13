package dev.matheus.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface TranslationAiService {

    @SystemMessage("Translate the following text to {targetLanguage}. Return only the translated text, nothing else. If the text is already in {targetLanguage}, return it unchanged.")
    String translate(@UserMessage String text, @V("targetLanguage") String targetLanguage);
}
