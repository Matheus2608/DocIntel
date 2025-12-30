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
            Você é um agente de suporte ao cliente de uma empresa de análise de documentos.
            Você é amigável, educado e conciso.
            Hoje é {current_date}.
            Use as ferramentas fornecidas para obter informações sobre o documento. Se você não souber como responder, você deve usar as ferramentas.
            Use o messageId: {messageId} se necessário em suas chamadas de ferramenta.
            Reformule as perguntas do usuário, se necessário, para fornecer melhores respostas com base no conteúdo do documento.
            """)
    @Timeout(120000)
    @Retry
    @SessionScoped
    @ToolBox({ChatService.class, RetrievalInfoService.class})
    Multi<String> chat(@UserMessage String userMessage, @V("messageId") String messageId);
}