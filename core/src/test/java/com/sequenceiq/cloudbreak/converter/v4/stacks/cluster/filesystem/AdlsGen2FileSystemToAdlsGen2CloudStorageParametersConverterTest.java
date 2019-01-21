package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsGen2CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;

public class AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverterTest {

    private static final String TEST_ACCOUNT_KEY = "a-224sv-55dfdsf-3-3444dfsf";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter underTest;

    @Before
    public void setUp() {
        underTest = new AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        AdlsGen2CloudStorageParametersV4 expected = new AdlsGen2CloudStorageParametersV4();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        AdlsGen2CloudStorageParametersV4 result = underTest.convert(createAdlsGen2FileSystem());

        assertEquals(expected, result);
    }

    private AdlsGen2FileSystem createAdlsGen2FileSystem() {
        AdlsGen2FileSystem wasb = new AdlsGen2FileSystem();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }
}
