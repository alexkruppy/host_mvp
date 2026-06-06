package com.example.rest.service;

import com.example.rest.model.Video;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class StreamingService {

    private static final Logger log = LoggerFactory.getLogger(StreamingService.class);

    public StreamingResult streamVideo(Video video, String rangeHeader) throws IOException {
        Path filePath = Path.of(video.getFilePath());
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new FileNotFoundException("Video file not found: " + video.getFilePath());
        }

        long fileLength = file.length();
        String contentType = video.getContentType() != null ? video.getContentType() : "video/mp4";

        if (rangeHeader == null) {
            return new StreamingResult(null, 0, fileLength - 1, fileLength, contentType, file, fileLength);
        }

        String[] ranges = rangeHeader.replace("bytes=", "").split("-");
        long start = Long.parseLong(ranges[0]);
        long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileLength - 1;
        long contentLength = end - start + 1;

        return new StreamingResult("bytes " + start + "-" + end + "/" + fileLength,
                start, end, contentLength, contentType, file, fileLength);
    }

    public void writeChunk(OutputStream outputStream, File file, long start, long end) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {
            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = end - start + 1;
            int bytesRead;
            while (remaining > 0 && (bytesRead = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                bos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
            bos.flush();
        }
    }

    public record StreamingResult(String contentRange, long start, long end,
                                  long contentLength, String contentType, File file, long fileLength) {}
}
