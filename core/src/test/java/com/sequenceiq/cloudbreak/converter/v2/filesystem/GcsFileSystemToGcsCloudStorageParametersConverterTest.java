package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsCloudStorageParameters;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GcsFileSystemToGcsCloudStorageParametersConverterTest {

    private static final String TEST_SERVICE_ACCOUNT_EMAIL = "someserviceaccount@email.com";

    private GcsFileSystemToGcsCloudStorageParametersConverter underTest;

    @Before
    public void setUp() {
        underTest = new GcsFileSystemToGcsCloudStorageParametersConverter();
    }

    @Test
    public void testConvertCheckEveryParamPassedProperly() {
        GcsCloudStorageParameters expected = new GcsCloudStorageParameters();
        expected.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);

        GcsCloudStorageParameters result = underTest.convert(createGcsFileSystem());

        assertEquals(result, expected);
    }

    private GcsFileSystem createGcsFileSystem() {
        GcsFileSystem gcs = new GcsFileSystem();
        gcs.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);
        return gcs;
    }

}