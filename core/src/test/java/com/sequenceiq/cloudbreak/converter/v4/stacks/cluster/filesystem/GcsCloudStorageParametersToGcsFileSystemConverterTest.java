package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.common.type.filesystem.GcsFileSystem;

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

    private GcsCloudStorageV4Parameters createGcsCloudStorageParameters() {
        GcsCloudStorageV4Parameters gcs = new GcsCloudStorageV4Parameters();
        gcs.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);
        return gcs;
    }

}