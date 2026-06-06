package com.example.rest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Service
public class FfmpegService {

    private static final Logger log = LoggerFactory.getLogger(FfmpegService.class);

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffmpeg.ffprobe.path:ffprobe}")
    private String ffprobePath;

    @Async
    public CompletableFuture<String> transcodeToHls(String inputFilePath, String outputDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path outputPath = Path.of(outputDir);
                Files.createDirectories(outputPath);

                String playlistPath = outputPath.resolve("index.m3u8").toString();

                ProcessBuilder pb = new ProcessBuilder(
                        ffmpegPath,
                        "-i", inputFilePath,
                        "-codec:v", "libx264",
                        "-codec:a", "aac",
                        "-start_number", "0",
                        "-hls_time", "10",
                        "-hls_list_size", "0",
                        "-hls_segment_filename", outputPath.resolve("segment_%03d.ts").toString(),
                        "-f", "hls",
                        playlistPath
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("ffmpeg: {}", line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("FFmpeg exited with code " + exitCode);
                }

                log.info("HLS transcoding completed: {}", playlistPath);
                return playlistPath;

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("FFmpeg transcoding failed", e);
            }
        });
    }

    public double getVideoDuration(String filePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "csv=p=0",
                    filePath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    return Double.parseDouble(line.trim());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("ffprobe exited with code {}, returning 0", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Failed to get video duration: {}", e.getMessage());
        }
        return 0;
    }

    public String generateThumbnail(String inputFilePath, String outputDir) {
        try {
            Path outputPath = Path.of(outputDir);
            Files.createDirectories(outputPath);
            String thumbnailPath = outputPath.resolve("thumbnail.jpg").toString();

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-i", inputFilePath,
                    "-ss", "00:00:05",
                    "-vframes", "1",
                    "-q:v", "2",
                    thumbnailPath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            return thumbnailPath;
        } catch (IOException | InterruptedException e) {
            log.warn("Failed to generate thumbnail: {}", e.getMessage());
            return null;
        }
    }
}
