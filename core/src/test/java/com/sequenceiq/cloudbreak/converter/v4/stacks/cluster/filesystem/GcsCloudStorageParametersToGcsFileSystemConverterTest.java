package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.common.api.cloudstorage.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;

public class GcsCloudStorageParametersToGcsFileSystemConverterTest {

    private static final String TEST_SERVICE_ACCOUNT_EMAIL = "someserviceaccount@email.com";

    private GcsCloudStorageParametersV4ToGcsFileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new GcsCloudStorageParametersV4ToGcsFileSystemConverter();
    }

    @Test
    public void testConvertCheckEveryParamPassedProperly() {
        GcsFileSystem expected = new GcsFileSystem();
        expected.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);

        GcsFileSystem result = underTest.convert(createGcsCloudStorageParameters());

        assertEquals(result, expected);
    }

    private GcsCloudStorageV1Parameters createGcsCloudStorageParameters() {
        GcsCloudStorageV1Parameters gcs = new GcsCloudStorageV1Parameters();
        gcs.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);
        return gcs;
    }

}