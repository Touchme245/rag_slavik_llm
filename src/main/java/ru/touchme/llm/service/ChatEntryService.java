package ru.touchme.llm.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.touchme.llm.model.ChatEntry;
import ru.touchme.llm.model.Role;
import ru.touchme.llm.repository.ChatRepository;

@Service
@RequiredArgsConstructor
public class ChatEntryService {

    private final ChatRepository chatRepository;

    @Transactional
    public void addChatEntry(Long chatId, String message, Role role) {
        var chat = chatRepository.findById(chatId).orElseThrow(EntityNotFoundException::new);
        var entry = ChatEntry.builder()
                .content(message)
                .role(role)
                .build();
        chat.addEntry(entry);
    }

}
