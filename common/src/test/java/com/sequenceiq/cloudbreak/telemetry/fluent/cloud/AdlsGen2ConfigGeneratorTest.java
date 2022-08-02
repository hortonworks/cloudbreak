package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class AdlsGen2ConfigGeneratorTest {

    private final AdlsGen2ConfigGenerator underTest = new AdlsGen2ConfigGenerator();

    @Test
    public void testGenerateStorageConfigWhenNoAccountIdInThePath() {
        String loc = "abfs://logs@.dfs.core.windows.net";
        CloudbreakServiceException actual = Assertions.assertThrows(CloudbreakServiceException.class, () -> underTest.generateStorageConfig(loc));
        Assertions.assertEquals("Invalid ADLS storage location: " + loc, actual.getMessage());
    }

    @Test
    public void testGenerateStorageConfigWhenValidPath() {
        String loc = "abfs://data@cdprestrictedenv1.dfs.core.windows.net/path/sub-directory";
        AdlsGen2Config adlsGen2Config = underTest.generateStorageConfig(loc);
        Assertions.assertEquals("cdprestrictedenv1", adlsGen2Config.getAccount());
        Assertions.assertEquals("data", adlsGen2Config.getFileSystem());
        Assertions.assertEquals("/path/sub-directory", adlsGen2Config.getFolderPrefix());
    }

    @Test
    public void testGenerateStorageConfigWhenEmptyPath() {
        String loc = "abfs://data@cdprestrictedenv1.dfs.core.windows.net/";
        AdlsGen2Config adlsGen2Config = underTest.generateStorageConfig(loc);
        Assertions.assertEquals("cdprestrictedenv1", adlsGen2Config.getAccount());
        Assertions.assertEquals("data", adlsGen2Config.getFileSystem());
        Assertions.assertEquals("/", adlsGen2Config.getFolderPrefix());

        loc = "abfs://data@cdprestrictedenv1.dfs.core.windows.net";
        adlsGen2Config = underTest.generateStorageConfig(loc);
        Assertions.assertEquals("cdprestrictedenv1", adlsGen2Config.getAccount());
        Assertions.assertEquals("data", adlsGen2Config.getFileSystem());
        Assertions.assertEquals("", adlsGen2Config.getFolderPrefix());
    }
}
