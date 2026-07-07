package com.bfrost.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService{

    private final Path uploadRoot;
    private final String baseUrl;

    public LocalStorageService(
            @Value("${bfrost.storage.upload-dir}") String uploadDir,
            @Value("${bfrost.storage.base-url}") String baseUrl
    ) throws IOException {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        Files.createDirectories(uploadRoot);
    }

    @Override
    public String store(MultipartFile file, String folder) {
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        Path destination = uploadRoot.resolve(folder).resolve(filename);
        try {
            Files.createDirectories(destination.getParent());
            file.transferTo(destination);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
        return baseUrl + "/uploads/" + folder + "/" + filename;
    }

    @Override
    public void delete(String url) {
        String path = url.replace(baseUrl + "/uploads/", "");
        Path file = uploadRoot.resolve(path);
        try { Files.deleteIfExists(file); } catch (IOException ignored) {}
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
