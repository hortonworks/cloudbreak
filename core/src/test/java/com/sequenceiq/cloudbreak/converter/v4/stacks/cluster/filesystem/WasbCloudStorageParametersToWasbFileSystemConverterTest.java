package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.WasbCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.WasbCloudStorageParametersV4ToWasbFileSystemConverter;
import com.sequenceiq.cloudbreak.services.filesystem.WasbFileSystem;

public class WasbCloudStorageParametersToWasbFileSystemConverterTest {

    private static final String TEST_ACCOUNT_KEY = "SKIAIKPVZ4DHPE4OHRGQ";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private static final boolean TEST_IS_SECURE_VALUE = true;

    private WasbCloudStorageParametersV4ToWasbFileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new WasbCloudStorageParametersV4ToWasbFileSystemConverter();
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

    private WasbCloudStorageParametersV4 createWasbCloudStorageParameters() {
        WasbCloudStorageParametersV4 wasb = new WasbCloudStorageParametersV4();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        wasb.setSecure(TEST_IS_SECURE_VALUE);
        return wasb;
    }

}