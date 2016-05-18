package com.sequenceiq.it.ssh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.util.MockFileSystem;
import org.apache.sshd.common.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class MockFileSystemFactory implements FileSystemFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockFileSystemFactory.class);

    @Override
    public FileSystem createFileSystem(Session session) throws IOException {
        return createMockFileSystem();
    }

    private FileSystem createMockFileSystem() {
        return new MockFileSystem("mockfs") {
            @Override
            public Path getPath(String first, String... more) {
                String fileName = new File(first).toPath().getFileName().toString();
                try {
                    ClassPathResource classPathResource = new ClassPathResource(fileName);
                    InputStream inputStream = classPathResource.getInputStream();
                    File tempFile = new File(fileName);
                    try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                        IOUtils.copy(inputStream, outputStream);
                    } catch (IOException e) {
                        LOGGER.error("can't write " + fileName, e);
                    }
                    return tempFile.toPath();
                } catch (IOException e) {
                    LOGGER.info("can't retrieve path from classpath, let's return with a file path from working directory");
                    return new File(first).toPath().getFileName();
                }
            }
        };
    }
}
