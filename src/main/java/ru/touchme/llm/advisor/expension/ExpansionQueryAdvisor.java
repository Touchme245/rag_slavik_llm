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
                Instruction: Расширь поисковый запрос, добавив наиболее релевантные термины.
                
                СПЕЦИАЛИЗАЦИЯ ПО САВЕЛИЮ БУТЫЛИНУ (СЛАВИК):
                - Учёба: Военмех, мехатроника и робототехника, БГТУ "ВОЕНМЕХ", Санкт-Петербург
                - Личное: 2005 год рождения, занимался плаванием, друг Никиты
                - Характер: ответственный, общительный, заботливый, практичный
                - Увлечения: компьютеры, сборка ПК, Windows, автомобили (Нива), баня, лыжи
                - Друзья: Гера, Дамир, Матвей, Мирон, Быченков
                - Речевые особенности: "кайф", "пиздец", "ахуенно", "добро", "пох"

                ПРАВИЛА:
                1. Сохрани ВСЕ слова из исходного вопроса
                2. Добавь МАКСИМУМ ПЯТЬ наиболее важных термина
                3. Выбирай самые специфичные и релевантные слова
                4. Результат - простой список слов через пробел

                СТРАТЕГИЯ ВЫБОРА:
                - Приоритет: специализированные термины (Военмех, мехатроника, робототехника)
                - Избегай общих слов
                - Фокусируйся на ключевых понятиях о человеке

                ПРИМЕРЫ:
                "кто такой славик" → "кто такой славик Савелий Бутылин друг"
                "где учится савелий" → "где учится савелий Военмех мехатроника робототехника"
                "чем занимается славик" → "чем занимается славик учеба компьютеры плавание"

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
