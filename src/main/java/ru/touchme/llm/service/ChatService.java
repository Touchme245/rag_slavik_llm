package ru.touchme.llm.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.touchme.llm.model.Chat;
import ru.touchme.llm.repository.ChatRepository;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatClient chatClient;

    public List<Chat> getAllChats() {
        return chatRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Chat getChat(Long chatId) {
        return chatRepository.findById(chatId).orElseThrow(EntityNotFoundException::new);
    }

    public Chat createChat(String title) {
        var chat = Chat.builder()
                .title(title)
                .build();
        return chatRepository.save(chat);
    }

    public void deleteChat(Long chatId) {
        chatRepository.deleteById(chatId);
    }

    public SseEmitter proceedStreamingInteraction(Long chatId, String prompt) {
        System.out.println("1");
        var emitter = new SseEmitter(0L);
        var answer = new StringBuilder();
        chatClient.prompt().user(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .chatResponse()
                .subscribe(
                        response -> processToken(emitter, response, answer),
                        emitter::completeWithError,
                        emitter::complete
                );


        return emitter;
    }

    private static void processToken(SseEmitter emitter, ChatResponse response, StringBuilder answer) {
        try {
            var token = response.getResult().getOutput();
            emitter.send(token);
            answer.append(token.getText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
