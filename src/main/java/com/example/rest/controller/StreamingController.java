package com.example.rest.controller;

import com.example.rest.model.User;
import com.example.rest.model.Video;
import com.example.rest.repository.UserRepository;
import com.example.rest.service.StreamingService;
import com.example.rest.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/api/stream")
public class StreamingController {

    private static final Logger log = LoggerFactory.getLogger(StreamingController.class);

    private final VideoService videoService;
    private final StreamingService streamingService;
    private final UserRepository userRepository;

    public StreamingController(VideoService videoService, StreamingService streamingService,
                               UserRepository userRepository) {
        this.videoService = videoService;
        this.streamingService = streamingService;
        this.userRepository = userRepository;
    }

    @GetMapping("/video/{id}")
    public void streamVideo(@PathVariable Long id,
                            @RequestHeader(value = "Range", required = false) String rangeHeader,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Authentication authentication) throws IOException {

        Optional<Video> optVideo = videoService.getVideoEntity(id);
        if (optVideo.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        Video video = optVideo.get();

        if (video.isPremium()) {
            boolean authorized = false;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent() && userOpt.get().isPremiumActive()) {
                    authorized = true;
                }
            }
            if (!authorized) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("Premium subscription required");
                return;
            }
        }

        try {
            StreamingService.StreamingResult result = streamingService.streamVideo(video, rangeHeader);

            if (rangeHeader != null) {
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                response.setHeader("Content-Range", result.contentRange());
            } else {
                response.setStatus(HttpStatus.OK.value());
            }

            response.setHeader(HttpHeaders.CONTENT_TYPE, result.contentType());
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.contentLength()));
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

            streamingService.writeChunk(response.getOutputStream(), result.file(), result.start(), result.end());
            response.flushBuffer();

        } catch (IOException e) {
            log.error("Error streaming video {}: {}", id, e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @GetMapping("/hls/{videoId}/{filename:.+}")
    public void streamHls(@PathVariable Long videoId,
                          @PathVariable String filename,
                          HttpServletResponse response,
                          Authentication authentication) throws IOException {

        Optional<Video> optVideo = videoService.getVideoEntity(videoId);
        if (optVideo.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        Video video = optVideo.get();

        if (video.isPremium()) {
            boolean authorized = false;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent() && userOpt.get().isPremiumActive()) {
                    authorized = true;
                }
            }
            if (!authorized) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("Premium subscription required");
                return;
            }
        }

        if (video.getHlsPath() == null) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.getWriter().write("HLS transcoding in progress");
            return;
        }

        Path hlsPath = Path.of(video.getHlsPath()).getParent().resolve(filename);
        if (!Files.exists(hlsPath)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        String contentType = filename.endsWith(".m3u8") ? "application/vnd.apple.mpegurl"
                : filename.endsWith(".ts") ? "video/MP2T"
                : "application/octet-stream";

        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        Files.copy(hlsPath, response.getOutputStream());
        response.flushBuffer();
    }
}
