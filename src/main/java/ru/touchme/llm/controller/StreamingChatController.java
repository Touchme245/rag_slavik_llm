package ru.touchme.llm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.touchme.llm.service.ChatService;

@RestController
@RequiredArgsConstructor
public class StreamingChatController {

    private final ChatService chatService;

    @GetMapping(value = "/chat-stream/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askModel(@PathVariable Long chatId,
                               @RequestParam String userPrompt,
                               @AuthenticationPrincipal UserDetails principal) {
        return chatService.proceedStreamingInteraction(chatId, principal.getUsername(), userPrompt);
    }
}
