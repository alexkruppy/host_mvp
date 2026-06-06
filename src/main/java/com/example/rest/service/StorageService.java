package com.example.rest.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${storage.local.upload-dir:uploads}")
    private String uploadDir;

    @Value("${storage.local.hls-dir:hls}")
    private String hlsDir;

    private Path uploadPath;
    private Path hlsPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        hlsPath = Paths.get(hlsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            Files.createDirectories(hlsPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directories", e);
        }
    }

    public String storeFile(MultipartFile file) {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = uploadPath.resolve(filename);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + filename, e);
        }
    }

    public Path getUploadPath() {
        return uploadPath;
    }

    public Path getHlsPath() {
        return hlsPath;
    }

    public Path resolveUpload(String filePath) {
        return uploadPath.resolve(filePath);
    }

    public Path resolveHls(String hlsPathStr) {
        return hlsPath.resolve(hlsPathStr);
    }

    public Path getFilePath(String fullPath) {
        return Paths.get(fullPath);
    }
}
