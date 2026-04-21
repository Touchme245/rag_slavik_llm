package ru.touchme.llm.advisor.rag;


import lombok.Builder;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.touchme.llm.advisor.expension.ExpansionQueryAdvisor.ENRICHED_QUESTION;

@Builder
public class RagAdvisor implements BaseAdvisor {

    private final VectorStore vectorStore;

    @Builder.Default
    private final BM25RerankEngine rerankEngine = BM25RerankEngine.builder().build();

    @Builder.Default
    private final SearchRequest searchRequest = SearchRequest.builder()
            .topK(4)
            .similarityThreshold(0.62)
            .build();

    private final int order;

    private static final PromptTemplate PROMPT_TEMPLATE = new PromptTemplate("""
            Context: {context}
            Question: {question}
            """);

    public static RagAdvisorBuilder builder(VectorStore vectorStore) {
        return new RagAdvisorBuilder().vectorStore(vectorStore);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        var originalUserQuestion = chatClientRequest.prompt().getUserMessage().getText();
        var queryToRag = chatClientRequest.context().getOrDefault(ENRICHED_QUESTION, originalUserQuestion).toString();
        var initialDocumentsToFind = searchRequest.getTopK();
        var request = SearchRequest.from(searchRequest).query(queryToRag)
                .topK(initialDocumentsToFind * 2)
                .build();

        var documents = vectorStore.similaritySearch(request);
        if (isEmpty(documents)) {
            return chatClientRequest.mutate().context("CONTEXT", "ТУТ ПУСТО - ни один окумент не был найден").build();
        }

        var rerankedDocuments = rerankEngine.rerank(documents, queryToRag, initialDocumentsToFind);

        var llmContext = rerankedDocuments.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));
        var finalUserPrompt = PROMPT_TEMPLATE.render(Map.of(
                "context", llmContext,
                "question", originalUserQuestion
        ));
        return chatClientRequest.mutate().prompt(chatClientRequest.prompt().augmentUserMessage(finalUserPrompt)).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
