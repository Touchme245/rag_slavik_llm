package ru.touchme.llm.configuration;

import lombok.Builder;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import ru.touchme.llm.model.ChatEntry;
import ru.touchme.llm.repository.ChatRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public class PostgresChatMemory implements ChatMemory {

    private final ChatRepository chatMemoryRepository;
    private final int maxMessages;

    @Override
    public void add(String chatId, List<Message> messages) {
        var chat = chatMemoryRepository.findById(Long.valueOf(chatId)).orElseThrow();
        for (var message : messages) {
            chat.addEntry(ChatEntry.toChatEntry(message));
        }
        chatMemoryRepository.save(chat);
    }

    @Override
    public List<Message> get(String chatId) {
        var chat = chatMemoryRepository.findById(Long.valueOf(chatId)).orElseThrow();
        int messagesToSkip = Math.max(0, chat.getHistory().size() - maxMessages);
        return chat.getHistory()
                .stream()
                .skip(messagesToSkip)
                .map(ChatEntry::toMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String s) {
        // not implemnted
    }
}
