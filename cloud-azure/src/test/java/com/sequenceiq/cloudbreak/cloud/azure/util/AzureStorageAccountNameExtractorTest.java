package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AzureStorageAccountNameExtractorTest {

    @InjectMocks
    private AzureStorageAccountNameExtractor underTest;

    @Test
    void testStorageAccountNameExtractionFromLocation() {
        String location = "logs@testcdpstorage.dfs.core.windows.net";

        String result = underTest.extractStorageAccountNameIfNecessary(location);

        assertEquals("testcdpstorage", result);
    }

    @Test
    void testStorageAccountNameExtractionFromFullLocation() {
        String location = "abfs://logs@testcdpstorage.dfs.core.windows.net";

        String result = underTest.extractStorageAccountNameIfNecessary(location);

        assertEquals("testcdpstorage", result);
    }

    @Test
    void testStorageAccountNameExtractionNotNeeded() {
        String location = "testcdpstorage";

        String result = underTest.extractStorageAccountNameIfNecessary(location);

        assertEquals("testcdpstorage", result);
    }

    @Test
    void testStorageAccountNameExtractionNull() {
        String location = null;

        String result = underTest.extractStorageAccountNameIfNecessary(location);

        assertNull(result);
    }

}