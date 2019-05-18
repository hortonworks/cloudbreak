package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.common.type.filesystem.WasbFileSystem;

public class WasbFileSystemToWasbCloudStorageParametersConverterTest {

    private static final String TEST_ACCOUNT_KEY = "SKIAIKPVZ4DHPE4OHRGQ";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private WasbFileSystemToWasbCloudStorageParametersV4Converter underTest;

    @Before
    public void setUp() {
        underTest = new WasbFileSystemToWasbCloudStorageParametersV4Converter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        WasbCloudStorageV4Parameters expected = new WasbCloudStorageV4Parameters();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        WasbCloudStorageV4Parameters result = underTest.convert(createWasbFileSystem());

        assertEquals(expected, result);
    }

    private WasbFileSystem createWasbFileSystem() {
        WasbFileSystem wasb = new WasbFileSystem();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }

}