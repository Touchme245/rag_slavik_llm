package ru.touchme.llm.command_line_runner;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import ru.touchme.llm.model.DocumentType;
import ru.touchme.llm.model.LoadedDocument;
import ru.touchme.llm.repository.DocumentRespository;

import java.util.Arrays;

import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.touchme.llm.model.DocumentType.TXT;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentLoaderService implements CommandLineRunner {

    private final DocumentRespository documentRespository;

    private final ResourcePatternResolver resolver;

    private final VectorStore vectorStore;

    @SneakyThrows
    public void loadDocuments() {
        log.info("Starting document loading process");

        var resources = Arrays.stream(resolver.getResources("classpath:/knowledgebase/**/*.txt")).toList();
        log.info("Found {} .txt files in knowledgebase", resources.size());

        if (isEmpty(resources)) {
            log.warn("No .txt files found in knowledgebase directory");
            return;
        }

        resources.stream()
                .map(resource -> Pair.of(resource, calculateContentHash(resource)))
                .filter(pair -> {
                    boolean exists = !documentRespository.existsByFilenameAndContentHash(
                            pair.getFirst().getFilename(),
                            pair.getSecond()
                    );
                    if (!exists) {
                        log.info("File {} already loaded (hash match), skipping", pair.getFirst().getFilename());
                    }
                    return exists;
                })
                .forEach(pair -> {
                    var resource = pair.getFirst();
                    log.info("Processing new document: {}", resource.getFilename());

                    try {
                        log.debug("Reading document: {}", resource.getFilename());
                        var documents = new TextReader(resource).get();
//                        log.info("Document {} has {} characters", resource.getFilename(), documents.get(0).getContent().length());

                        log.debug("Creating text splitter with chunk size 200");
                        var splitter = TokenTextSplitter.builder()
                                .withChunkSize(200)
                                .build();

                        log.debug("Splitting document into chunks");
                        var chunks = splitter.apply(documents);
                        log.info("Document {} split into {} chunks", resource.getFilename(), chunks.size());

                        log.debug("Storing chunks in vector store for: {}", resource.getFilename());
                        vectorStore.accept(chunks);
                        log.info("Successfully stored chunks in vector store for: {}", resource.getFilename());

                        var loadedDocument = LoadedDocument.builder()
                                .documentType(TXT)
                                .chunkCount(chunks.size())
                                .filename(resource.getFilename())
                                .contentHash(pair.getSecond())
                                .build();

                        log.debug("Saving document metadata to database for: {}", resource.getFilename());
                        documentRespository.save(loadedDocument);
                        log.info("Document {} loaded successfully", resource.getFilename());

                    } catch (Exception e) {
                        log.error("Failed to process document: {}", resource.getFilename(), e);
                        log.error("Error details: {}", e.getMessage());
                        throw new RuntimeException("Failed to process document: " + resource.getFilename(), e);
                    }
                });

        log.info("Document loading process completed");
    }

    @SneakyThrows
    private String calculateContentHash(Resource resource) {
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }

    @Override
    public void run(String... args) throws Exception {
        loadDocuments();
    }
}
