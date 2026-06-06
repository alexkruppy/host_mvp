package com.example.rest.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "hls_path")
    private String hlsPath;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "duration")
    private Double duration;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "views")
    private Long views = 0L;

    @Column(name = "hls_ready")
    private boolean hlsReady = false;

    public Video() {}

    public Video(String title, String description, String filePath, Long fileSize, String contentType, boolean isPremium, User user) {
        this.title = title;
        this.description = description;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.isPremium = isPremium;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getHlsPath() { return hlsPath; }
    public void setHlsPath(String hlsPath) { this.hlsPath = hlsPath; }
    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }
    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public Long getViews() { return views; }
    public void setViews(Long views) { this.views = views; }
    public boolean isHlsReady() { return hlsReady; }
    public void setHlsReady(boolean hlsReady) { this.hlsReady = hlsReady; }
}
