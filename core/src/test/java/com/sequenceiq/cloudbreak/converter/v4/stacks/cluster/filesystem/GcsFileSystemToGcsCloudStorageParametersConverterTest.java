package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.GcsFileSystemToGcsCloudStorageParametersV4Converter;
import com.sequenceiq.cloudbreak.services.filesystem.GcsFileSystem;

public class GcsFileSystemToGcsCloudStorageParametersConverterTest {

    private static final String TEST_SERVICE_ACCOUNT_EMAIL = "someserviceaccount@email.com";

    private GcsFileSystemToGcsCloudStorageParametersV4Converter underTest;

    @Before
    public void setUp() {
        underTest = new GcsFileSystemToGcsCloudStorageParametersV4Converter();
    }

    @Test
    public void testConvertCheckEveryParamPassedProperly() {
        GcsCloudStorageParametersV4 expected = new GcsCloudStorageParametersV4();
        expected.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);

        GcsCloudStorageParametersV4 result = underTest.convert(createGcsFileSystem());

        assertEquals(result, expected);
    }

    private GcsFileSystem createGcsFileSystem() {
        GcsFileSystem gcs = new GcsFileSystem();
        gcs.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);
        return gcs;
    }

}