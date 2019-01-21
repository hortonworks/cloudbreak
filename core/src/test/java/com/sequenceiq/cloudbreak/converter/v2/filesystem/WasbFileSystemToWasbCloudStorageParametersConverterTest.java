package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import com.sequenceiq.cloudbreak.services.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbCloudStorageParameters;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WasbFileSystemToWasbCloudStorageParametersConverterTest {

    private static final String TEST_ACCOUNT_KEY = "SKIAIKPVZ4DHPE4OHRGQ";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private WasbFileSystemToWasbCloudStorageParametersConverter underTest;

    @Before
    public void setUp() {
        underTest = new WasbFileSystemToWasbCloudStorageParametersConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        WasbCloudStorageParameters expected = new WasbCloudStorageParameters();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        WasbCloudStorageParameters result = underTest.convert(createWasbFileSystem());

        assertEquals(expected, result);
    }

    private WasbFileSystem createWasbFileSystem() {
        WasbFileSystem wasb = new WasbFileSystem();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }

}