package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.common.api.cloudstorage.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;

public class GcsFileSystemToGcsCloudStorageParametersConverterTest {

    private static final String TEST_SERVICE_ACCOUNT_EMAIL = "someserviceaccount@email.com";

    private GcsFileSystemToGcsCloudStorageParametersV4Converter underTest;

    @Before
    public void setUp() {
        underTest = new GcsFileSystemToGcsCloudStorageParametersV4Converter();
    }

    @Test
    public void testConvertCheckEveryParamPassedProperly() {
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