package com.vpnexues.svc.service;

import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.util.Slugify;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores admin-uploaded photos as real files under the configured uploads
 * directory (served back at /uploads/** by UploadStorageConfig) instead of
 * embedding base64 data URLs in the database. Files are named after the member
 * (slugified name, e.g. nageswaran-duraiswamy.jpg) with a counter suffix on
 * collision; a UUID is only the fallback when no name is given.
 */
@Service
public class ContentUploadService {

    private static final Logger log = LoggerFactory.getLogger(ContentUploadService.class);

    /** Content type -> file extension; the only types accepted from the admin UI. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private static final long MAX_BYTES = 5 * 1024 * 1024;

    /** Subfolders of the uploads dir content may write to (never a path from the client). */
    private static final java.util.Set<String> FOLDERS = java.util.Set.of("team", "news", "products");

    private final Path root;

    public ContentUploadService(@Value("${app.uploads.dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Validates the upload and writes it as /uploads/&lt;folder&gt;/&lt;base&gt;.&lt;ext&gt;.
     *
     * @param memberName optional display name/title; slugified into the filename so the
     *                   path reads like the old /images/team/nageswaran.jpg
     * @param replaceUrl previous /uploads/... path (best-effort delete of the old file)
     * @param folder     "team" or "news" — the only writable subfolders
     */
    public String storePhoto(MultipartFile file, String memberName, String replaceUrl, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No image file received.");
        }
        String subFolder = folder == null || folder.isBlank() ? "team" : folder.toLowerCase();
        if (!FOLDERS.contains(subFolder)) {
            throw new BadRequestException("Unsupported upload folder.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new BadRequestException("Unsupported image type. Use JPEG, PNG, WebP or GIF.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image is too large (max 5 MB).");
        }
        byte[] bytes = file.getBytes();
        if (!matchesSignature(contentType, bytes)) {
            throw new BadRequestException("File content is not a valid image.");
        }
        Path dir = root.resolve(subFolder);
        Files.createDirectories(dir);
        Path target = uniqueTarget(dir, Slugify.slugify(memberName), extension);
        Files.write(target, bytes);
        deleteReplaced(replaceUrl);
        return "/uploads/" + subFolder + "/" + target.getFileName();
    }

    /** slug.jpg, or slug-2.jpg / slug-3.jpg ... when taken; UUID when no name. */
    private Path uniqueTarget(Path dir, String slug, String extension) {
        String base = slug.isEmpty() ? UUID.randomUUID().toString() : slug;
        Path target = dir.resolve(base + extension);
        int counter = 2;
        while (Files.exists(target)) {
            target = dir.resolve(base + "-" + counter + extension);
            counter++;
        }
        return target;
    }

    /** Best-effort cleanup of the photo this upload replaces (never outside the uploads dir). */
    private void deleteReplaced(String replaceUrl) {
        // Accept /uploads/... and absolute URLs that contain it (prod with API_BASE_URL set).
        int marker = replaceUrl == null ? -1 : replaceUrl.indexOf("/uploads/");
        if (marker < 0) {
            return;
        }
        String path = replaceUrl.substring(marker + "/uploads/".length());
        try {
            Path resolved = root.resolve(path).normalize();
            if (resolved.startsWith(root) && Files.isRegularFile(resolved)) {
                Files.deleteIfExists(resolved);
            }
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not delete replaced upload {}: {}", replaceUrl, ex.getMessage());
        }
    }

    /** Magic-byte check so a renamed text file cannot slip through as an image. */
    private boolean matchesSignature(String contentType, byte[] h) {
        return switch (contentType) {
            case "image/jpeg" ->
                h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "image/png" ->
                h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "image/gif" -> h.length >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
            case "image/webp" ->
                h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                        && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            default -> false;
        };
    }
}
