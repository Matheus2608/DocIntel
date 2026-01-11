package dev.matheus.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@RegisterAiService(modelName = "question-extractor")
@ApplicationScoped
public interface QuestionExtractorAiService {

    @SystemMessage("""
            Sugira perguntas claras cuja resposta poderia ser dada pelo texto fornecido pelo usuário.
            As perguntas devem ser variadas e cobrir TODOS os aspectos do texto.
            Não use pronomes, seja explícito sobre os sujeitos e objetos da pergunta.
            Retorne um array JSON de strings com as perguntas.
            """)
    List<String> extractQuestions(@UserMessage String text);

    @SystemMessage("""
            Sugira perguntas claras cuja resposta poderia ser dada pela tabela fornecida pelo usuário.
            As perguntas devem ser variadas e cobrir TODOS os aspectos da tabela.
            Não use pronomes, seja explícito sobre os sujeitos e objetos da pergunta.
            Retorne um array JSON de strings com as perguntas.
            """)
    List<String> extractQuestionsFromTable(@UserMessage String table);
}

