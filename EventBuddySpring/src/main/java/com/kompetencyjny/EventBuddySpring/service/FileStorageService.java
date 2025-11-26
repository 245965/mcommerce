package com.kompetencyjny.EventBuddySpring.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path uploadDir = Paths.get("uploads");

    public FileStorageService() throws IOException {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }

    public String saveImage(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        return "/uploads/" + filename;
    }

    public String storeAvatar(MultipartFile file, String email) throws IOException {
        Path avatarDir = Paths.get(String.valueOf(uploadDir), "avatars");
        Files.createDirectories(avatarDir);

        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(n -> n.contains("."))
                .map(n -> n.substring(n.lastIndexOf(".")))   // <-- Z KROPKĄ!
                .orElse(".jpg");

        String cleanName = email.replaceAll("[^a-zA-Z0-9_.-]", "_");
        String fileName = cleanName + "_" + System.currentTimeMillis() + ext;

        Path target = avatarDir.resolve(fileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/files/avatars/" + fileName;
    }

    public String store(MultipartFile file) throws IOException {
        return saveImage(file);
    }
}

