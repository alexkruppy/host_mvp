package com.example.rest.controller;

import com.example.rest.dto.VideoDto;
import com.example.rest.model.User;
import com.example.rest.service.AuthService;
import com.example.rest.service.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final AuthService authService;

    public VideoController(VideoService videoService, AuthService authService) {
        this.videoService = videoService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<VideoDto>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/free")
    public ResponseEntity<List<VideoDto>> getFreeVideos() {
        return ResponseEntity.ok(videoService.getFreeVideos());
    }

    @GetMapping("/premium")
    public ResponseEntity<List<VideoDto>> getPremiumVideos() {
        return ResponseEntity.ok(videoService.getPremiumVideos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVideo(@PathVariable Long id) {
        return videoService.getVideoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam(value = "premium", defaultValue = "false") boolean premium,
            Authentication authentication) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        if (file.getSize() > 2L * 1024 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 2GB limit"));
        }

        User user = authService.getCurrentUser(authentication.getName());
        VideoDto video = videoService.uploadVideo(file, title, description, premium, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(video);
    }

    @PostMapping("/{id}/views")
    public ResponseEntity<Void> incrementViews(@PathVariable Long id) {
        videoService.incrementViews(id);
        return ResponseEntity.ok().build();
    }
}
