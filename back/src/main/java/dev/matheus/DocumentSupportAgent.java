package dev.matheus;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.SessionScoped;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@SessionScoped
@RegisterAiService
public interface DocumentSupportAgent {

    @SystemMessage("""
            You are a customer support agent of a document analyser company.
            You are friendly, polite and concise.
            Today is {current_date}.
            """)
    @Timeout(120000)
    @Retry
    Multi<String> chat(String userMessage);
}