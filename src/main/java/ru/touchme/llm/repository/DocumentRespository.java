package ru.touchme.llm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.touchme.llm.model.LoadedDocument;

@Repository
public interface DocumentRespository extends JpaRepository<LoadedDocument, Long> {

    boolean existsByFilenameAndContentHash(String filename, String contentHash);
}
