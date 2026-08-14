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

    @GetMapping(value = "/stream")
    public ResponseEntity<Resource> streamReel() throws Exception {
        File file = new File("generated/reel.mp4");
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"reel.mp4\"")
                .header(org.springframework.http.HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    @GetMapping(value = "/image/stream", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> streamImage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String mrp,
            @RequestParam(required = false) String image,
            @RequestParam(required = false) Integer discount) throws Exception {

        File file = new File("generated/post_image.jpg");
        if (title != null || price != null || image != null || !file.exists()) {
            String targetTitle = (title != null && !title.isEmpty()) ? title : "🔥 TODAY'S SPECIAL OFFER";
            String targetPrice = (price != null && !price.isEmpty()) ? price : "999";
            String targetMrp = (mrp != null && !mrp.isEmpty()) ? mrp : "1999";
            String targetImage = (image != null && !image.isEmpty()) ? image : "https://dummyimage.com/600x600/ffffff/000000.jpg&text=Amazon+Deal";
            int disc = (discount != null) ? discount : 50;

            videoService.createPostImage(targetImage, targetTitle, targetPrice, targetMrp, disc, 1000);
            file = new File("generated/post_image.jpg");
        }

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @GetMapping(value = "/image/generate", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> generatePostImage(
            @RequestParam(defaultValue = "🔥 TODAY'S SPECIAL OFFER") String title,
            @RequestParam(defaultValue = "999") String price,
            @RequestParam(defaultValue = "1999") String mrp,
            @RequestParam(required = false) String image,
            @RequestParam(defaultValue = "50") int discount) throws Exception {

        String targetImage = (image != null && !image.isEmpty()) ? image : "https://dummyimage.com/600x600/ffffff/000000.jpg&text=Amazon+Deal";
        java.awt.image.BufferedImage img = videoService.renderPostImage(targetImage, title, price, mrp, discount, 0);

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(baos.toByteArray());
    }

    @GetMapping(value = "/carousel-image/{index}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> streamCarouselImage(@PathVariable int index) throws Exception {
        File file = new File("generated/carousel_slide_" + index + ".jpg");
        if (!file.exists()) {
            file = new File("generated/post_image.jpg");
        }
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}