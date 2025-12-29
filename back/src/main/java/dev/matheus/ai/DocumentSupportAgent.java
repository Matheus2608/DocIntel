package dev.matheus.ai;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.matheus.service.ChatService;
import dev.matheus.service.RetrievalInfoService;
import io.quarkiverse.langchain4j.ToolBox;
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
            In case you need more information, use the MessageId: {messageId} on your tool calls.
            Reformulate user questions if necessary to provide better answers based on the document content.
            """)
    @Timeout(120000)
    @Retry
    @SessionScoped
    @ToolBox({ChatService.class, RetrievalInfoService.class})
    Multi<String> chat(@UserMessage String userMessage, @V("messageId") String messageId);
}