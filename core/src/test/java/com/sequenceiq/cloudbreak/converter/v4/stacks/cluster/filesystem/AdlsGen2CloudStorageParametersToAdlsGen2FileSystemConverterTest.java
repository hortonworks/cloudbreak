package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.common.api.cloudstorage.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;

public class AdlsGen2CloudStorageParametersToAdlsGen2FileSystemConverterTest {

    private static final String TEST_ACCOUNT_KEY = "SKIAIKPVZ4DHPE4OHRGQ";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private AdlsGen2CloudStorageParametersV4ToAdlsGen2FileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new AdlsGen2CloudStorageParametersV4ToAdlsGen2FileSystemConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        AdlsGen2FileSystem expected = new AdlsGen2FileSystem();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        AdlsGen2FileSystem result = underTest.convert(createAdlsGen2CloudStorageParameters());

        assertEquals(expected, result);
    }

    private AdlsGen2CloudStorageV1Parameters createAdlsGen2CloudStorageParameters() {
        AdlsGen2CloudStorageV1Parameters wasb = new AdlsGen2CloudStorageV1Parameters();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }
}
