//package dev.matheus.ai;
//
//import dev.langchain4j.model.output.structured.Description;
//import dev.langchain4j.service.SystemMessage;
//import dev.langchain4j.service.UserMessage;
//import io.quarkiverse.langchain4j.RegisterAiService;
//
//@RegisterAiService(modelName = "search-strategy")
//public interface SearchStrategyDeterminerAiService {
//
//    @SystemMessage("""
//            Você é um especialista em RAG (Retrieval-Augmented Generation). Sua tarefa é determinar a melhor estratégia de busca para uma pergunta do usuário.
//            Você deve escolher uma das seguintes estratégias:
//            - HYPOTHETICAL_QUESTIONS: Use quando a pergunta do usuário é vaga ou muito curta.
//            - FAKE_ANSWERS: Use quando a pergunta do usuário é específica e detalhada.
//            - BOTH: Use quando a pergunta do usuário é moderadamente detalhada.
//
//            Responda apenas com o nome da estratégia.
//            """)
//    SearchStrategy determineSearchStrategy(@UserMessage String userQuestion);
//
//    enum SearchStrategy {
//        @Description("Estrategia que usa o match entre questoes hipoteticas e a pergunta do usuario. Reformule bem a pergunta para melhores resultados.")
//        HYPOTHETICAL_QUESTIONS,
//        @Description("Estrategia que cria uma resposta falsa e a utiliza para fazer o match com os segmentos.")
//        FAKE_ANSWERS,
//        @Description("Usa ambas as estrategias.")
//        BOTH
//    }
//}
//
