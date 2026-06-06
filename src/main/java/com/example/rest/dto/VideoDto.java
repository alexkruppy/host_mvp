package com.example.rest.dto;

import com.example.rest.model.Video;

public class VideoDto {
    private Long id;
    private String title;
    private String description;
    private Long fileSize;
    private String contentType;
    private boolean isPremium;
    private boolean hlsReady;
    private String hlsPath;
    private String thumbnailPath;
    private Double duration;
    private Long views;
    private String uploadedAt;
    private String uploadedBy;

    public static VideoDto from(Video video) {
        VideoDto dto = new VideoDto();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setFileSize(video.getFileSize());
        dto.setContentType(video.getContentType());
        dto.setPremium(video.isPremium());
        dto.setHlsReady(video.isHlsReady());
        dto.setHlsPath(video.getHlsPath());
        dto.setThumbnailPath(video.getThumbnailPath());
        dto.setDuration(video.getDuration());
        dto.setViews(video.getViews());
        dto.setUploadedAt(video.getUploadedAt() != null ? video.getUploadedAt().toString() : null);
        dto.setUploadedBy(video.getUser().getFullName());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public boolean isHlsReady() { return hlsReady; }
    public void setHlsReady(boolean hlsReady) { this.hlsReady = hlsReady; }
    public String getHlsPath() { return hlsPath; }
    public void setHlsPath(String hlsPath) { this.hlsPath = hlsPath; }
    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }
    public Long getViews() { return views; }
    public void setViews(Long views) { this.views = views; }
    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
