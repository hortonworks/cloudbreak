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
}
