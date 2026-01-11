package dev.matheus.ai;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

//@RegisterAiService(modelName = "search-parameters")
//public interface SearchParametersDeterminerAiService {
//
//    @SystemMessage("""
//            Você é um especialista em RAG (Retrieval-Augmented Generation). Sua tarefa é determinar os melhores parâmetros de busca para uma pergunta do usuário.
//            Baseado na pergunta, determine:
//            - minSimilarity: A similaridade mínima para a busca de embeddings (entre 0.7 e 0.9).
//            - minScore: O score mínimo para o re-ranking (entre 0.5 e 0.8).
//            - maxResults: O número máximo de resultados a serem retornados (entre 3 e 10).
//            """)
//    SearchParameters determineSearchParameters(@UserMessage String userQuestion);
//
//    @Description("Parameters for a RAG search.")
//    class SearchParameters {
//        @Description("The minimum similarity for the embedding search (between 0.7 and 0.9).")
//        public double minSimilarity;
//        @Description("The minimum score for the re-ranking (between 0.5 and 0.8).")
//        public double minScore;
//        @Description("The maximum number of results to be returned (between 3 and 10).")
//        public int maxResults;
//    }
//}

