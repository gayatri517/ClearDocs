package com.cleardocs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Service
@Slf4j
public class FileStorageService {

    @Value("${cleardocs.storage.base-path:/tmp/cleardocs/documents}")
    private String basePath;

    private Path storageRoot;

    @PostConstruct
    public void init() {
        storageRoot = Paths.get(basePath);
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory: " + basePath, e);
        }
    }

    public String store(MultipartFile file, String referenceNumber) {
        String originalFilename = StringUtils.cleanPath(
            file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) extension = originalFilename.substring(dotIdx);

        String storedName = referenceNumber + "_" + System.currentTimeMillis() + extension;
        Path targetPath = storageRoot.resolve(storedName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }

        log.info("Stored file {} -> {}", originalFilename, targetPath);
        return targetPath.toString();
    }

    public byte[] load(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + filePath, e);
        }
    }

    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("Could not delete file: {}", filePath);
        }
    }
}
