package com.editflowimg.backend.controller;

import com.editflowimg.backend.entity.ImageEntity;
import com.editflowimg.backend.entity.UserEntity;
import com.editflowimg.backend.exception.AppException;
import com.editflowimg.backend.service.ImageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping
    public ImageEntity upload(@RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal UserEntity user) throws IOException {
        return imageService.upload(file, user);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id,
                                           @AuthenticationPrincipal UserEntity user) throws IOException {
        ImageEntity image = imageService.findById(id);

        if (!image.getUser().getId().equals(user.getId())) {
            throw new AppException("Acesso negado", HttpStatus.FORBIDDEN);
        }

        byte[] fileBytes = imageService.loadImage(image);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + image.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(fileBytes);
    }
}
