/**
 * Copyright (c) 2025 Bit Learning. All rights reserved.
 * This software is the confidential and proprietary information of Bit Learning.
 * You shall not disclose such confidential information and shall use it only in
 * accordance with the terms of the license agreement you entered into with Bit Learning.
 */
package thitkho.userservice.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import thitkho.userservice.exception.InvalidFileException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryUtils {
    private final Cloudinary cloudinary;

    public Pair<String, String> uploadFile(File file, String destination) throws IOException {
        if (file == null || !file.exists() || file.length() == 0) {
            throw new InvalidFileException("File is empty or does not exist");
        }

        Map uploadResult =
                cloudinary
                        .uploader()
                        .upload(
                                file,
                                ObjectUtils.asMap(
                                        "folder",
                                        "bit-learning" + destination,
                                        "resource_type",
                                        "auto"));

        String url = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        return Pair.of(url, publicId);
    }

    public boolean deleteFile(String publicId) throws IOException {
        Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return "ok".equals(result.get("result"));
    }

    /**
     * Upload MultipartFile directly to Cloudinary.
     * Writes to a temp file, uploads, then deletes the temp file.
     *
     * @param file        the multipart file to upload
     * @param destination sub-folder path under "bit-learning/", e.g. "/chat/attachments"
     * @return Pair of (secure_url, public_id)
     */
    public Pair<String, String> uploadMultipartFile(MultipartFile file, String destination)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or null");
        }

        String originalName =
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext =
                originalName.contains(".")
                        ? originalName.substring(originalName.lastIndexOf("."))
                        : "";
        Path tempFile = Files.createTempFile("cl_upload_", ext);
        try {
            file.transferTo(tempFile.toFile());
            return uploadFile(tempFile.toFile(), destination);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void deleteFolderByPublicId(String publicId) {
        if (publicId == null || !publicId.contains("/")) {
            throw new IllegalArgumentException("Invalid public id, cannot extract folder");
        }

        String folderPath = publicId.substring(0, publicId.lastIndexOf("/"));

        try {
            List<String> resourceTypes = Arrays.asList("video", "raw", "image");

            for (String type : resourceTypes) {
                cloudinary
                        .api()
                        .deleteResourcesByPrefix(
                                folderPath, ObjectUtils.asMap("resource_type", type));
            }

            cloudinary.api().deleteFolder(folderPath, ObjectUtils.emptyMap());

            log.info("Deleted folder: {}", folderPath);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete Cloudinary folder: " + folderPath, e);
        }
    }

    public Pair<String, String> uploadVideoAndSeparateSegments(File video, String destination)
            throws IOException, InterruptedException {
        Path tempOutputDir = Files.createTempDirectory("hls_output_" + UUID.randomUUID());

        String cloudFolder = "bit-learning" + destination + "/" + "segments";
        StringBuilder finalPlaylistUrl = new StringBuilder();

        try {
            String outputPattern = tempOutputDir.resolve("segment_%03d.ts").toString();
            String playlistPath = tempOutputDir.resolve("playlist.m3u8").toString();

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "ffmpeg",
                            "-i",
                            video.getAbsolutePath(),
                            "-c:v",
                            "copy",
                            "-c:a",
                            "copy",
                            "-f",
                            "hls",
                            "-hls_time",
                            "10",
                            "-hls_list_size",
                            "0",
                            "-hls_segment_filename",
                            outputPattern,
                            playlistPath);

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    System.out.println(reader.readLine());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0)
                throw new RuntimeException("FFmpeg failed when separating segments: " + exitCode);

            Pair<String, String> uploadOriginalVideoRes = this.uploadFile(video, destination);

            try (Stream<Path> paths = Files.walk(tempOutputDir)) {
                paths.filter(Files::isRegularFile)
                        .forEach(
                                path -> {
                                    try {
                                        String filename = path.getFileName().toString();

                                        Map uploadParams =
                                                ObjectUtils.asMap(
                                                        "folder", cloudFolder,
                                                        "resource_type", "raw",
                                                        "use_filename", true,
                                                        "unique_filename", false,
                                                        "overwrite", true);

                                        Map result =
                                                cloudinary
                                                        .uploader()
                                                        .upload(path.toFile(), uploadParams);

                                        if (filename.endsWith(".m3u8")) {
                                            finalPlaylistUrl.append(
                                                    (String) result.get("secure_url"));
                                        }
                                    } catch (IOException e) {
                                        throw new RuntimeException("Upload failed for " + path, e);
                                    }
                                });
            }

            return Pair.of(finalPlaylistUrl.toString(), uploadOriginalVideoRes.getSecond());

        } finally {
            try (Stream<Path> walk = Files.walk(tempOutputDir)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }
}
