package ru.touchme.llm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.touchme.llm.service.ChatService;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/")
    public String mainPage(ModelMap model,
                           @AuthenticationPrincipal UserDetails principal) {
        model.addAttribute("chats", chatService.getAllChats(principal.getUsername()));
        return "chat";
    }

    @GetMapping("/chat/{chatId}")
    public String chat(@PathVariable("chatId") Long chatId,
                       ModelMap model,
                       @AuthenticationPrincipal UserDetails principal) {
        var username = principal.getUsername();
        model.addAttribute("chats", chatService.getAllChats(username));
        model.addAttribute("chat", chatService.getChat(chatId, username));
        return "chat";
    }

    @PostMapping("/chat/new")
    public String createChat(@RequestParam(name = "title") String title,
                             @AuthenticationPrincipal UserDetails principal) {
        var chat = chatService.createChat(title, principal.getUsername());
        return "redirect:/chat/" + chat.getId();
    }

    @PostMapping("/chat/{chatId}/delete")
    public String deleteChat(@PathVariable Long chatId,
                             @AuthenticationPrincipal UserDetails principal) {
        chatService.deleteChat(chatId, principal.getUsername());
        return "redirect:/";
    }
}
