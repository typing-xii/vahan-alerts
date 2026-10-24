package com.vahanalerts.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentAttachmentStorage {

    private static final Pattern STORAGE_KEY =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|jpg|png)");

    private final Path storageDirectory;
    private final DataSize maxFileSize;

    public DocumentAttachmentStorage(
            @Value("${vahan.documents.storage-directory:./data/document-files}") String storageDirectory,
            @Value("${vahan.documents.max-file-size:5MB}") DataSize maxFileSize) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
    }

    public StoredDocumentAttachment store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentAttachmentException("Choose a non-empty PDF, JPEG, or PNG file.");
        }
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new InvalidDocumentAttachmentException("The attachment exceeds the configured size limit.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new DocumentStorageException("The attachment could not be read.", exception);
        }

        FileType fileType = detectType(content);
        if (fileType == null) {
            throw new InvalidDocumentAttachmentException("Only valid PDF, JPEG, and PNG files are accepted.");
        }

        String key = UUID.randomUUID() + "." + fileType.extension;
        Path destination = resolveKey(key);
        try {
            Files.createDirectories(storageDirectory);
            Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DocumentStorageException("The attachment could not be stored.", exception);
        }
        return new StoredDocumentAttachment(key, fileType.mediaType, content.length);
    }

    public Resource load(String key) {
        Path path = resolveKey(key);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    public void delete(String key) {
        Path path = resolveKey(key);
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new DocumentStorageException("The attachment could not be deleted.", exception);
        }
    }

    private Path resolveKey(String key) {
        if (key == null || !STORAGE_KEY.matcher(key).matches()) {
            throw new DocumentStorageException("The attachment reference is invalid.",
                    new IllegalArgumentException("Invalid generated attachment key."));
        }
        Path resolved = storageDirectory.resolve(key).normalize();
        if (!resolved.startsWith(storageDirectory)) {
            throw new DocumentStorageException("The attachment reference is invalid.",
                    new IllegalArgumentException("Attachment path is outside storage directory."));
        }
        return resolved;
    }

    private FileType detectType(byte[] content) {
        if (startsWith(content, new byte[] {'%', 'P', 'D', 'F', '-'})) {
            return new FileType("pdf", "application/pdf");
        }
        if (startsWith(content, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) {
            return new FileType("jpg", "image/jpeg");
        }
        if (startsWith(content, new byte[] {
                (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10
        })) {
            return new FileType("png", "image/png");
        }
        return null;
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private record FileType(String extension, String mediaType) {
    }
}
