package ru.touchme.llm.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.touchme.llm.model.Chat;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
}
