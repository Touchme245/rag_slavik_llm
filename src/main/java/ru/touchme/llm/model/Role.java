package ru.touchme.llm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Arrays;
import java.util.Objects;

@Getter
@AllArgsConstructor
public enum Role {

    USER("user") {
        @Override
        Message getMessage(String prompt) {
            return new UserMessage(prompt);
        }
    },

    ASSISTANT("assistant") {
        @Override
        Message getMessage(String prompt) {
            return new AssistantMessage(prompt);
        }
    },

    SYSTEM("system") {
        @Override
        Message getMessage(String prompt) {
            return new SystemMessage(prompt);
        }
    };

    private final String role;

    public static Role getRole(String roleName) {
        return Arrays.stream(values())
                .filter(role -> Objects.equals(role.getRole(), roleName))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    abstract Message getMessage(String prompt);
}
