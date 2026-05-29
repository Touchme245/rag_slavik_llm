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
import ru.touchme.llm.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatClient chatClient;

    public List<Chat> getAllChats(String username) {
        var user = userRepository.findByUsername(username).orElseThrow(
                () -> new EntityNotFoundException("Пользователь %s не найден".formatted(username))
        );
        return user.getChats();
    }

    public Chat getChat(Long chatId, String username) {
        var chat = chatRepository.findById(chatId).orElseThrow(EntityNotFoundException::new);
        validateChatPermissions(chat, username);
        return chat;
    }

    public Chat createChat(String title, String username) {
        var user = userRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        var chat = Chat.builder()
                .title(title)
                .user(user)
                .build();
        return chatRepository.save(chat);
    }

    public void deleteChat(Long chatId, String username) {
        System.out.println("чат с ид" + chatId);
        System.out.println("имя" + username);
        var chat = chatRepository.findById(chatId).orElseThrow(EntityNotFoundException::new);
        validateChatPermissions(chat, username);
        System.out.println("удаляю");
        chatRepository.delete(chat);
        System.out.println("удалил");
    }

    public SseEmitter proceedStreamingInteraction(Long chatId, String username, String prompt) {
        var chat = chatRepository.findById(chatId).orElseThrow(EntityNotFoundException::new);
        validateChatPermissions(chat, username);

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

    private void validateChatPermissions(Chat chat, String username) {
        var chatOwner = chat.getUser();
        if (!Objects.equals(chatOwner.getUsername(), username)) {
            throw new IllegalArgumentException("не твой чат не лезь");
        }
    }
}
