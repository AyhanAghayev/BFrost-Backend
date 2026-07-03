package com.bfrost.backend.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/{folder}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@PathVariable String folder,
                                      @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File must not be empty");
        String url = storageService.store(file, folder);
        return Map.of("url", url);
    }
}
