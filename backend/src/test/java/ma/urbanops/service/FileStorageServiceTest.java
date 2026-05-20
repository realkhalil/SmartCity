package ma.urbanops.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir Path tempDir;

    @Test
    void storeLoadAndDeleteFile_shouldUseConfiguredDirectory() throws Exception {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        service.init();

        String fileName = service.storeFile(new MockMultipartFile(
                "photo", "road.jpg", "image/jpeg", "content".getBytes()));

        assertNotNull(fileName);
        assertTrue(fileName.endsWith(".jpg"));
        assertTrue(Files.exists(tempDir.resolve(fileName)));
        assertEquals(tempDir.resolve(fileName).normalize(), service.loadFile(fileName));

        service.deleteFile(fileName);

        assertFalse(Files.exists(tempDir.resolve(fileName)));
    }

    @Test
    void storeFile_whenFileIsNullOrEmpty_shouldReturnNull() {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        service.init();

        assertNull(service.storeFile(null));
        assertNull(service.storeFile(new MockMultipartFile("photo", new byte[0])));
    }

    @Test
    void storeFile_whenPathTraversalDetected_shouldThrow() {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        service.init();

        MockMultipartFile file = new MockMultipartFile("photo", "../bad.jpg", "image/jpeg", "x".getBytes());

        assertThrows(RuntimeException.class, () -> service.storeFile(file));
    }

    @Test
    void init_whenDirectoryCannotBeCreated_shouldThrow() throws Exception {
        Path filePath = tempDir.resolve("not-a-directory");
        Files.writeString(filePath, "content");
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", filePath.toString());

        assertThrows(RuntimeException.class, service::init);
    }
}
