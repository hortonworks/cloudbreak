package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;

class GcsFileSystemToGcsCloudStorageParametersConverterTest {

    private static final String TEST_SERVICE_ACCOUNT_EMAIL = "someserviceaccount@email.com";

    private GcsFileSystemToGcsCloudStorageParametersV4Converter underTest = new GcsFileSystemToGcsCloudStorageParametersV4Converter();

    @Test
    void testConvertCheckEveryParamPassedProperly() {
        GcsCloudStorageV1Parameters expected = new GcsCloudStorageV1Parameters();
        expected.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);

        GcsCloudStorageV1Parameters result = underTest.convert(createGcsFileSystem());

        assertEquals(result, expected);
    }

    private GcsFileSystem createGcsFileSystem() {
        GcsFileSystem gcs = new GcsFileSystem();
        gcs.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);
        return gcs;
    }

}