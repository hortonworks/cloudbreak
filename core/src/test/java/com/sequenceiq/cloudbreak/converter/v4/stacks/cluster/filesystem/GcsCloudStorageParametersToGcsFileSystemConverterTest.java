package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.GcsCloudStorageParametersV4ToGcsFileSystemConverter;
import com.sequenceiq.cloudbreak.services.filesystem.GcsFileSystem;

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

    private GcsCloudStorageParametersV4 createGcsCloudStorageParameters() {
        GcsCloudStorageParametersV4 gcs = new GcsCloudStorageParametersV4();
        gcs.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);
        return gcs;
    }

}