package com.sequenceiq.cloudbreak.service.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Response;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class FileSystemSupportMatrixServiceTest {

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @InjectMocks
    private FileSystemSupportMatrixService underTest;

    @BeforeEach
    void before() throws IOException {
        String specifications = FileReaderUtils.readFileFromClasspath("definitions/cloud-storage-support-matrix.json");
        when(cloudbreakResourceReaderService.resourceDefinition("cloud-storage-support-matrix")).thenReturn(specifications);
        underTest.init();
    }

    @Test
    void testWithVersion265() {
        Set<CloudStorageSupportedV4Response> matrix = underTest.getCloudStorageMatrix("2.6.5");
        assertEquals(3L, matrix.size());
        matrix.forEach(response -> assertTrue(!response.getFileSystemType().isEmpty()));
        assertEquals(2L, matrix.stream().filter(response -> "AZURE".equalsIgnoreCase(response.getProvider())).findFirst().get().getFileSystemType().size());
    }

    @Test
    void testWithVersion260() {
        Set<CloudStorageSupportedV4Response> matrix = underTest.getCloudStorageMatrix("2.6.0");
        assertEquals(2L, matrix.size());
        matrix.forEach(response -> assertTrue(!response.getFileSystemType().isEmpty()));
        assertEquals(2L, matrix.stream().filter(response -> "AZURE".equalsIgnoreCase(response.getProvider())).findFirst().get().getFileSystemType().size());
    }

    @Test
    void testWithVersion250() {
        Set<CloudStorageSupportedV4Response> matrix = underTest.getCloudStorageMatrix("2.5.0");
        assertTrue(matrix.isEmpty());
    }

    @Test
    void testWithVersion300() {
        Set<CloudStorageSupportedV4Response> matrix = underTest.getCloudStorageMatrix("3.0.0");
        assertEquals(3L, matrix.size());
        matrix.forEach(response -> assertTrue(!response.getFileSystemType().isEmpty()));
        assertEquals(3L, matrix.stream().filter(response -> "AZURE".equalsIgnoreCase(response.getProvider())).findFirst().get().getFileSystemType().size());
    }
}