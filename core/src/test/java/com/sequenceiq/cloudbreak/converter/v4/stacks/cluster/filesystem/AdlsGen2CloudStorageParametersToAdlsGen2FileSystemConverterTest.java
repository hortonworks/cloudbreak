package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsGen2CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.AdlsGen2CloudStorageParametersV4ToAdlsGen2FileSystemConverter;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;

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

    private AdlsGen2CloudStorageParametersV4 createAdlsGen2CloudStorageParameters() {
        AdlsGen2CloudStorageParametersV4 wasb = new AdlsGen2CloudStorageParametersV4();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }
}
