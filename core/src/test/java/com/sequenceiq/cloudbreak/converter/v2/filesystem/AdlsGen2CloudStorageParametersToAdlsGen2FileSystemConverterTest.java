package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2CloudStorageParameters;

public class AdlsGen2CloudStorageParametersToAdlsGen2FileSystemConverterTest {

    private static final String TEST_ACCOUNT_KEY = "SKIAIKPVZ4DHPE4OHRGQ";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private AdlsGen2CloudStorageParametersToAdlsGen2FileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new AdlsGen2CloudStorageParametersToAdlsGen2FileSystemConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        AdlsGen2FileSystem expected = new AdlsGen2FileSystem();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        AdlsGen2FileSystem result = underTest.convert(createAdlsGen2CloudStorageParameters());

        assertEquals(expected, result);
    }

    private AdlsGen2CloudStorageParameters createAdlsGen2CloudStorageParameters() {
        AdlsGen2CloudStorageParameters wasb = new AdlsGen2CloudStorageParameters();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }
}
