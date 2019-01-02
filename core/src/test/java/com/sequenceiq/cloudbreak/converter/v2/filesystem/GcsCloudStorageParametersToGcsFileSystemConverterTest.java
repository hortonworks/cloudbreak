package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsCloudStorageParameters;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GcsCloudStorageParametersToGcsFileSystemConverterTest {

    private static final String TEST_SERVICE_ACCOUNT_EMAIL = "someserviceaccount@email.com";

    private GcsCloudStorageParametersToGcsFileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new GcsCloudStorageParametersToGcsFileSystemConverter();
    }

    @Test
    public void testConvertCheckEveryParamPassedProperly() {
        GcsFileSystem expected = new GcsFileSystem();
        expected.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);

        GcsFileSystem result = underTest.convert(createGcsCloudStorageParameters());

        assertEquals(result, expected);
    }

    private GcsCloudStorageParameters createGcsCloudStorageParameters() {
        GcsCloudStorageParameters gcs = new GcsCloudStorageParameters();
        gcs.setServiceAccountEmail(TEST_SERVICE_ACCOUNT_EMAIL);
        return gcs;
    }

}