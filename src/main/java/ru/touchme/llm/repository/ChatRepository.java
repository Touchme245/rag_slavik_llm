package ru.touchme.llm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.touchme.llm.model.Chat;
import ru.touchme.llm.model.User;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByIdAndUser(Long id, User user);
}
