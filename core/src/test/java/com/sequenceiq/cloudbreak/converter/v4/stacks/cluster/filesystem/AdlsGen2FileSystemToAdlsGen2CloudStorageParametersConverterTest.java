package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;

class AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverterTest {

    private static final String TEST_ACCOUNT_KEY = "a-224sv-55dfdsf-3-3444dfsf";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter();
    }

    @Test
    void testConvertCheckEveryParameterHasPassedProperly() {
        AdlsGen2CloudStorageV1Parameters expected = new AdlsGen2CloudStorageV1Parameters();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        AdlsGen2CloudStorageV1Parameters result = underTest.convert(createAdlsGen2FileSystem());

        assertEquals(expected, result);
    }

    private AdlsGen2FileSystem createAdlsGen2FileSystem() {
        AdlsGen2FileSystem adlsGen2FileSystem = new AdlsGen2FileSystem();
        adlsGen2FileSystem.setAccountKey(TEST_ACCOUNT_KEY);
        adlsGen2FileSystem.setAccountName(TEST_ACCOUNT_NAME);
        return adlsGen2FileSystem;
    }
}
