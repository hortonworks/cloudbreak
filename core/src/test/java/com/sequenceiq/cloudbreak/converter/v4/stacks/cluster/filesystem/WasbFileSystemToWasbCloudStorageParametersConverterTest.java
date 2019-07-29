package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;

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
        WasbCloudStorageV1Parameters expected = new WasbCloudStorageV1Parameters();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        WasbCloudStorageV1Parameters result = underTest.convert(createWasbFileSystem());

        assertEquals(expected, result);
    }

    private WasbFileSystem createWasbFileSystem() {
        WasbFileSystem wasb = new WasbFileSystem();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }

}