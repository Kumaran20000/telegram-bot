package com.example.telegram_bot.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.imageio.ImageIO;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.telegram_bot.model.Deal;
import com.example.telegram_bot.service.VideoGenerationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoController {

    private final VideoGenerationService videoService;

    @GetMapping(value = "/create", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> create(
            @RequestParam(defaultValue = "220 GSM Ultra-Soft Luxury AC Comforter Set Double Bed") String title,
            @RequestParam(defaultValue = "1749") String price,
            @RequestParam(defaultValue = "2999") String mrp,
            @RequestParam(defaultValue = "42") int discount,
            @RequestParam(required = false) String image,
            @RequestParam(required = false) Integer templateIndex) throws Exception {

        Deal deal = new Deal();
        deal.setTitle(title);
        deal.setPrice(price);
        deal.setMrp(mrp);
        deal.setDiscount(discount + "%");
        deal.setImage(image != null && !image.isEmpty() ? image : "src/main/resources/images/product.jpg");

        int tIndex = templateIndex != null ? templateIndex : -1;
        String path = videoService.createReel(deal, tIndex);

        File file = new File(path);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"reel.mp4\"")
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

    @GetMapping(value = "/scene/{sceneNumber}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getScenePreview(
            @PathVariable int sceneNumber,
            @RequestParam(defaultValue = "220 GSM Ultra-Soft Luxury AC Comforter Set Double Bed") String title,
            @RequestParam(defaultValue = "1749") String price,
            @RequestParam(defaultValue = "2999") String mrp,
            @RequestParam(defaultValue = "42") int discount,
            @RequestParam(required = false) String image) throws Exception {

        Deal deal = new Deal();
        deal.setTitle(title);
        deal.setPrice(price);
        deal.setMrp(mrp);
        deal.setDiscount(discount + "%");
        deal.setImage(image != null && !image.isEmpty() ? image : "src/main/resources/images/product.jpg");

        java.awt.image.BufferedImage img = videoService.renderScenePreview(deal, sceneNumber);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(baos.toByteArray());
    }

    @GetMapping(value = "/image/stream", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> streamImage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String mrp,
            @RequestParam(required = false) String image,
            @RequestParam(required = false) Integer discount) throws Exception {

        String targetTitle = (title != null && !title.isEmpty()) ? title : "220 GSM Ultra-Soft Luxury AC Comforter Set Double Bed";
        String targetPrice = (price != null && !price.isEmpty()) ? price : "1749";
        String targetMrp = (mrp != null && !mrp.isEmpty()) ? mrp : "2999";
        String targetImage = (image != null && !image.isEmpty()) ? image : "src/main/resources/images/product.jpg";
        int disc = (discount != null) ? discount : 42;

        videoService.createPostImage(targetImage, targetTitle, targetPrice, targetMrp, disc, 1250);
        File file = new File("generated/post_image.jpg");

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
            @RequestParam(defaultValue = "220 GSM Ultra-Soft Luxury AC Comforter Set Double Bed") String title,
            @RequestParam(defaultValue = "1749") String price,
            @RequestParam(defaultValue = "2999") String mrp,
            @RequestParam(required = false) String image,
            @RequestParam(defaultValue = "42") int discount) throws Exception {

        String targetImage = (image != null && !image.isEmpty()) ? image : "src/main/resources/images/product.jpg";
        java.awt.image.BufferedImage img = videoService.renderPostImage(targetImage, title, price, mrp, discount, 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);

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