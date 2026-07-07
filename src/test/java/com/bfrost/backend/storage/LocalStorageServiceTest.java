package com.bfrost.backend.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() throws IOException {
        storageService = new LocalStorageService(tempDir.toString(), "http://localhost:8080");
    }

    @Test
    void storesFileUnderTheRequestedFolder() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "data".getBytes());

        String url = storageService.store(file, "avatars");

        assertThat(url).startsWith("http://localhost:8080/uploads/avatars/");
        Path expectedDir = tempDir.resolve("avatars");
        assertThat(Files.list(expectedDir)).hasSize(1);
    }

    @Test
    void deleteIgnoresPathTraversalAttempt() {
        storageService.delete("http://localhost:8080/uploads/../../outside/evil.png");
    }
}
