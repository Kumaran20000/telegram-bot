package com.example.telegram_bot.controller;

import com.example.telegram_bot.service.VideoGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoController {

    private final VideoGenerationService videoService;

    @GetMapping(value = "/create", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> create() throws Exception {

        String path = videoService.createReel(
                "src/main/resources/images/product.jpg",
                "Sample Deal Product Title",
                "999"
        );

        File file = new File(path);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> streamReel() throws Exception {
        File file = new File("generated/reel.mp4");
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    @GetMapping(value = "/image/stream", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> streamImage() throws Exception {
        File file = new File("generated/post_image.jpg");
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}