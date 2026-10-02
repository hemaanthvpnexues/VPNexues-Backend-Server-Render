package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vpnexues.svc.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ContentUploadServiceTest {

    @TempDir
    Path tempDir;

    private static final byte[] PNG_HEADER = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13
    };

    private ContentUploadService service() {
        return new ContentUploadService(tempDir.toString());
    }

    @Test
    void storesValidPngUnderUploadsTeamAndReturnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_HEADER);
        String url = service().storePhoto(file, null, null, "team");

        assertTrue(url.startsWith("/uploads/team/"), "returned URL is served from /uploads/team/");
        assertTrue(url.endsWith(".png"), "extension follows the content type");
        Path stored = tempDir.resolve("team").resolve(url.substring("/uploads/team/".length()));
        assertTrue(Files.exists(stored), "file written to the configured uploads directory");
        assertEquals(Files.size(stored), file.getSize(), "bytes stored unchanged");
    }

    @Test
    void namesFileAfterTheMemberSlug() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_HEADER);
        String url = service().storePhoto(file, "Nageswaran Duraiswamy", null, "team");
        assertEquals("/uploads/team/nageswaran-duraiswamy.png", url, "filename reads like /images/team/<name>");
    }

    @Test
    void sameNameGetsCounterSuffixInsteadOfOverwriting() throws IOException {
        ContentUploadService service = service();
        String first = service.storePhoto(new MockMultipartFile("file", "a.png", "image/png", PNG_HEADER), "Libin K", null, "team");
        String second = service.storePhoto(new MockMultipartFile("file", "b.png", "image/png", PNG_HEADER), "Libin K", null, "team");
        assertEquals("/uploads/team/libin-k.png", first);
        assertEquals("/uploads/team/libin-k-2.png", second, "a different member with the same name never clobbers the first photo");
    }

    @Test
    void deletesTheReplacedUploadButNothingOutsideUploads() throws IOException {
        ContentUploadService service = service();
        String old = service.storePhoto(new MockMultipartFile("file", "old.png", "image/png", PNG_HEADER), "Nikhil", null, "team");
        Path oldPath = tempDir.resolve(old.substring("/uploads/".length()));
        assertTrue(Files.exists(oldPath));

        service.storePhoto(new MockMultipartFile("file", "new.png", "image/png", PNG_HEADER), "Nikhil", old, "team");
        assertTrue(!Files.exists(oldPath), "previous photo cleaned up on re-upload");

        // a traversal replace URL resolves outside the uploads dir and must never delete
        Path outside = tempDir.resolveSibling("outside-secret.txt");
        Files.write(outside, "keep".getBytes());
        try {
            service.storePhoto(new MockMultipartFile("file", "new2.png", "image/png", PNG_HEADER), "Nikhil",
                    "/uploads/../outside-secret.txt", "team");
            assertTrue(Files.exists(outside), "paths outside the uploads dir are never deleted");
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void storesNewsImagesUnderUploadsNews() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "news.png", "image/png", PNG_HEADER);
        String url = service().storePhoto(file, "Harvest Festival 2026", null, "news");
        assertEquals("/uploads/news/harvest-festival-2026.png", url);
        assertTrue(Files.exists(tempDir.resolve("news").resolve("harvest-festival-2026.png")));
    }

    @Test
    void storesProductImagesUnderUploadsProducts() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "prod.png", "image/png", PNG_HEADER);
        String url = service().storePhoto(file, "Organic Ginger", null, "products");
        assertEquals("/uploads/products/organic-ginger.png", url);
        assertTrue(Files.exists(tempDir.resolve("products").resolve("organic-ginger.png")));
    }

    @Test
    void rejectsUnknownUploadFolder() {
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", PNG_HEADER);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service().storePhoto(file, null, null, "../../etc"));
        assertTrue(ex.getMessage().contains("folder"));
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service().storePhoto(file, null, null, "team"));
        assertTrue(ex.getMessage().contains("Unsupported image type"));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service().storePhoto(file, null, null, "team"));
        assertTrue(ex.getMessage().contains("No image file"));
    }

    @Test
    void rejectsOversizedImage() {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG_HEADER, 0, big, 0, PNG_HEADER.length);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", big);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service().storePhoto(file, null, null, "team"));
        assertTrue(ex.getMessage().contains("too large"));
    }

    @Test
    void rejectsContentThatDoesNotMatchItsDeclaredType() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service().storePhoto(file, null, null, "team"));
        assertTrue(ex.getMessage().contains("not a valid image"));
    }
}
