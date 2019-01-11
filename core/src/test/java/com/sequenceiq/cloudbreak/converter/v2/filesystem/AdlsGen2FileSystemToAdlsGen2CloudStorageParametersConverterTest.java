package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2CloudStorageParameters;

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
        AdlsGen2CloudStorageParameters expected = new AdlsGen2CloudStorageParameters();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        AdlsGen2CloudStorageParameters result = underTest.convert(createAdlsGen2FileSystem());

        assertEquals(expected, result);
    }

    private AdlsGen2FileSystem createAdlsGen2FileSystem() {
        AdlsGen2FileSystem wasb = new AdlsGen2FileSystem();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }
}
