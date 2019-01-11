package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbCloudStorageParameters;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WasbCloudStorageParametersToWasbFileSystemConverterTest {

    private static final String TEST_ACCOUNT_KEY = "SKIAIKPVZ4DHPE4OHRGQ";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private static final boolean TEST_IS_SECURE_VALUE = true;

    private WasbCloudStorageParametersToWasbFileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new WasbCloudStorageParametersToWasbFileSystemConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        WasbFileSystem expected = new WasbFileSystem();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);
        expected.setSecure(TEST_IS_SECURE_VALUE);

        WasbFileSystem result = underTest.convert(createWasbCloudStorageParameters());

        assertEquals(expected, result);
    }

    private WasbCloudStorageParameters createWasbCloudStorageParameters() {
        WasbCloudStorageParameters wasb = new WasbCloudStorageParameters();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        wasb.setSecure(TEST_IS_SECURE_VALUE);
        return wasb;
    }

}