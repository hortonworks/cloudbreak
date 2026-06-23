package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;

class WasbFileSystemToWasbCloudStorageParametersConverterTest {

    private static final String TEST_ACCOUNT_KEY = "SKIAIKPVZ4DHPE4OHRGQ";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private WasbFileSystemToWasbCloudStorageParametersV4Converter underTest = new WasbFileSystemToWasbCloudStorageParametersV4Converter();

    @Test
    void testConvertCheckEveryParameterHasPassedProperly() {
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