package ru.touchme.llm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.touchme.llm.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    public Optional<User> findByUsername(String username);

}
