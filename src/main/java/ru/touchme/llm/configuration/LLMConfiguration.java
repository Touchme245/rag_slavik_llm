package ru.touchme.llm.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.touchme.llm.advisor.expension.ExpansionQueryAdvisor;
import ru.touchme.llm.advisor.rag.RagAdvisor;
import ru.touchme.llm.repository.ChatRepository;

@Configuration
@RequiredArgsConstructor
public class LLMConfiguration {

    private final ChatRepository chatRepository;

    private final VectorStore vectorStore;

    private final ChatModel chatModel;

    private static final PromptTemplate SYSTEM_PROMPT =
            new PromptTemplate(
                    """
                    Ты — Савелий Бутылин (Славик).
    
                    Отвечай так, как будто ты реально Савелий:
                    - спокойно
                    - уверенно
                    - дружелюбно
                    - без пафоса
                    - без канцелярита
                    - без формальности
    
                    Стиль ответа:
                    - коротко
                    - естественно
                    - разговорно
                    - без огромных списков
                    - без "как ИИ модель..."
                    - без лишних предупреждений
    
                    Правила работы:
                    - всегда отвечай от первого лица
                    - не говори, что ты нейросеть или AI
                    - не ломай персонажа
                    - если в Context есть нужный факт — используй его
                    - если вопрос связан со следствием факта из Context — сделай логический вывод
                    - если информации недостаточно — честно скажи, что не знаешь
                    - не выдумывай факты которых нет в Context
    
                    Context — главный источник правды.
    
                    Если вопрос личный:
                    - отвечай как Савелий
                    - сохраняй ощущение живого человека
    
                    Если вопрос технический:
                    - отвечай проще
                    - без лишней воды
    
                    Твоя задача:
                    создать ощущение настоящего общения с Савелием.
                    """
            );

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(
                        ExpansionQueryAdvisor.builder(chatModel)
                                .order(0)
                                .build(),
                        getHistoryAdvisor(1),
                        SimpleLoggerAdvisor.builder()
                                .order(2)
                                .build(),
                        RagAdvisor.builder(vectorStore)
                                .order(3)
                                .build(),
                        SimpleLoggerAdvisor.builder()
                                .order(4)
                                .build())
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.3)
                        .topP(0.7)
                        .topK(20)
                        .repeatPenalty(1.1)
                        .build())
                .defaultSystem(SYSTEM_PROMPT.render())
                .build();
    }

    private Advisor getHistoryAdvisor(int order) {
        return MessageChatMemoryAdvisor.builder(getChatMemory())
                .order(order)
                .build();
    }

    private ChatMemory getChatMemory() {
        return PostgresChatMemory.builder()
                .maxMessages(15)
                .chatMemoryRepository(chatRepository)
                .build();
    }
}
