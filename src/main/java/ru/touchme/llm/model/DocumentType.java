package ru.touchme.llm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentType {

    TXT("txt");

    private final String value;
}
