package com.editflowimg.backend.service;

import com.editflowimg.backend.entity.ImageEntity;
import com.editflowimg.backend.entity.UserEntity;
import com.editflowimg.backend.enums.ImageKind;
import com.editflowimg.backend.exception.AppException;
import com.editflowimg.backend.repository.ImageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final Path uploadDir = Paths.get("uploads");

    public ImageService(ImageRepository imageRepository) throws IOException {
        this.imageRepository = imageRepository;
        Files.createDirectories(uploadDir);
    }

    public ImageEntity upload(MultipartFile file, UserEntity user) throws IOException {
        String fileName = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(fileName);

        Files.write(filePath, file.getBytes());

        ImageEntity image = new ImageEntity();
        image.setUser(user);
        image.setKind(ImageKind.ORIGINAL);
        image.setOriginalName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSizeBytes(file.getSize());
        image.setStorageKey(filePath.toString());

        return imageRepository.save(image);
    }

    public ImageEntity findById(UUID id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new AppException("Imagem não encontrada", HttpStatus.NOT_FOUND));
    }

    public byte[] loadImage(ImageEntity image) throws IOException {
        Path path = Paths.get(image.storageKeyValue());
        return Files.readAllBytes(path);
    }
}