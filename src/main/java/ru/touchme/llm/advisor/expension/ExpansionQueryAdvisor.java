package ru.touchme.llm.advisor.expension;

import lombok.Builder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import java.util.Map;

@Builder
public class ExpansionQueryAdvisor implements BaseAdvisor {

    public static final String ENRICHED_QUESTION = "ENRICHED_QUESTION";
    public static final String ORIGINAL_QUESTION = "ORIGINAL_QUESTION";
    public static final String EXPANSION_RATIO = "EXPANSION_RATIO";

    private final int order;
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private static final PromptTemplate template = PromptTemplate.builder()
            .template("""
                   Instruction: Ты расширяешь поисковый запрос для поиска по личной переписке Савелия Бутылина (Славика).
                   Цель:
                   Помочь векторному поиску найти максимально релевантные сообщения.
                   Известные темы переписки:
                   Учёба:
                   Военмех, мехатроника, робототехника, университет, экзамен, зачёт, лабораторная
                   Автомобили:
                   Нива, машина, авто, права, вождение, ремонт
                   Компьютеры:
                   ПК, компьютер, Windows, сборка, железо, видеокарта
                   Отдых:
                   баня, алкоголь, шашлыки, тусовка, отдых
                   Спорт:
                   плавание, лыжи, велосипед, литербол
                   Друзья:
                   Гера, Дамир, Матвей, Мирон, Быченков
                   ПРАВИЛА:
                   1. Сохрани все слова исходного вопроса.
                   2. Добавляй только слова из темы вопроса.
                   3. Не смешивай темы.
                   4. Если вопрос уже конкретный — не добавляй ничего.
                   5. Максимум 3 дополнительных термина.
                   6. Ответ должен содержать только поисковый запрос.
                   Примеры:
                   Вопрос:
                   Где учится Славик
                   Ответ:
                   Где учится Славик Военмех мехатроника
                   Вопрос:
                   Какая машина у Славика
                   Ответ:
                   Какая машина у Славика Нива авто
                   Вопрос:
                   Любит ли Славик баню
                   Ответ:
                   Любит ли Славик баню алкоголь отдых
                   Вопрос:
                   Кто такой Славик
                   Ответ:
                   Кто такой Славик Савелий Бутылин
                   Question: {question}
                   Expanded query:
                    """).build();

    public static ExpansionQueryAdvisorBuilder builder(ChatModel chatModel) {
        return new ExpansionQueryAdvisorBuilder()
                .chatClient(
                        ChatClient.builder(chatModel)
                                .defaultOptions(
                                        OllamaChatOptions.builder()
                                                .temperature(0.0)
                                                .topK(1)
                                                .topP(0.1)
                                                .build()
                                )
                                .build()
                );
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        var userQuestion = chatClientRequest.prompt().getUserMessage().getText();
        var subQuestion = template.render(Map.of("question", userQuestion));
        var enrichedQuestion = chatClient
                .prompt()
                .user(subQuestion)
                .call()
                .content();
        var ration = enrichedQuestion.length() / (double) userQuestion.length();
        return chatClientRequest.mutate()
                .context(ENRICHED_QUESTION, enrichedQuestion)
                .context(ORIGINAL_QUESTION, userQuestion)
                .context(EXPANSION_RATIO, ration)
                .build();
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
