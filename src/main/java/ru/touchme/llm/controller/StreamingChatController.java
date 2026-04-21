package ru.touchme.llm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.touchme.llm.service.ChatService;

@RestController
@RequiredArgsConstructor
public class StreamingChatController {

    private final ChatService chatService;

    @GetMapping(value = "/chat-stream/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askModel(@PathVariable Long chatId, @RequestParam String userPrompt) {
        return chatService.proceedStreamingInteraction(chatId, userPrompt);
    }
}
