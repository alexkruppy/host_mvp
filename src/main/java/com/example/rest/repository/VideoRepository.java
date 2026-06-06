package com.example.rest.repository;

import com.example.rest.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserIdOrderByUploadedAtDesc(Long userId);

    @Query("SELECT v FROM Video v WHERE v.isPremium = false ORDER BY v.uploadedAt DESC")
    List<Video> findFreeVideos();

    @Query("SELECT v FROM Video v ORDER BY v.uploadedAt DESC")
    List<Video> findAllVideos();

    @Query("SELECT v FROM Video v WHERE v.isPremium = true ORDER BY v.uploadedAt DESC")
    List<Video> findPremiumVideos();
}
