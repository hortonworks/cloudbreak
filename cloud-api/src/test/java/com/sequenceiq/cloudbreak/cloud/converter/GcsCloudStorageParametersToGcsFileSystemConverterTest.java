package com.sequenceiq.cloudbreak.cloud.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.storage.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.common.type.filesystem.GcsFileSystem;

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