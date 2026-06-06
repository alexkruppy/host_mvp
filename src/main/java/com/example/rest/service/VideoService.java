package com.example.rest.service;

import com.example.rest.dto.VideoDto;
import com.example.rest.model.User;
import com.example.rest.model.Video;
import com.example.rest.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final FfmpegService ffmpegService;

    public VideoService(VideoRepository videoRepository, StorageService storageService,
                        FfmpegService ffmpegService) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.ffmpegService = ffmpegService;
    }

    public VideoDto uploadVideo(MultipartFile file, String title, String description,
                                boolean isPremium, User user) {
        String filePath = storageService.storeFile(file);

        Video video = new Video(title, description, filePath, file.getSize(),
                file.getContentType(), isPremium, user);
        Video saved = videoRepository.save(video);

        double duration = ffmpegService.getVideoDuration(filePath);
        saved.setDuration(duration);
        videoRepository.save(saved);

        Long videoId = saved.getId();
        String hlsDir = storageService.getHlsPath().resolve(videoId.toString()).toString();
        String thumbnailDir = storageService.getHlsPath().resolve(videoId.toString() + "_thumb").toString();

        CompletableFuture<String> hlsFuture = ffmpegService.transcodeToHls(filePath, hlsDir);
        hlsFuture.thenAccept(hlsPath -> {
            videoRepository.findById(videoId).ifPresent(v -> {
                v.setHlsPath(hlsPath);
                v.setHlsReady(true);
                videoRepository.save(v);
                log.info("HLS transcoding completed for video {}", videoId);
            });
        }).exceptionally(ex -> {
            log.error("HLS transcoding failed for video {}: {}", videoId, ex.getMessage());
            return null;
        });

        CompletableFuture.runAsync(() -> {
            String thumbPath = ffmpegService.generateThumbnail(filePath, thumbnailDir);
            if (thumbPath != null) {
                videoRepository.findById(videoId).ifPresent(v -> {
                    v.setThumbnailPath(thumbPath);
                    videoRepository.save(v);
                });
            }
        });

        return VideoDto.from(saved);
    }

    public List<VideoDto> getAllVideos() {
        return videoRepository.findAllVideos().stream()
                .map(VideoDto::from)
                .toList();
    }

    public List<VideoDto> getFreeVideos() {
        return videoRepository.findFreeVideos().stream()
                .map(VideoDto::from)
                .toList();
    }

    public List<VideoDto> getPremiumVideos() {
        return videoRepository.findPremiumVideos().stream()
                .map(VideoDto::from)
                .toList();
    }

    public List<VideoDto> getUserVideos(Long userId) {
        return videoRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                .map(VideoDto::from)
                .toList();
    }

    public Optional<VideoDto> getVideoById(Long id) {
        return videoRepository.findById(id).map(VideoDto::from);
    }

    public Optional<Video> getVideoEntity(Long id) {
        return videoRepository.findById(id);
    }

    public void incrementViews(Long id) {
        videoRepository.findById(id).ifPresent(video -> {
            video.setViews(video.getViews() + 1);
            videoRepository.save(video);
        });
    }
}
