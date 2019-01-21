package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.s3.S3CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.S3CloudStorageParametersV4ToS3FileSystemConverter;
import com.sequenceiq.cloudbreak.services.filesystem.S3FileSystem;

public class S3CloudStorageParametersToS3FileSystemConverterTest {

    private static final String TEST_INSTANCE_PROFILE = "i-123123rfa23";

    private S3CloudStorageParametersV4ToS3FileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new S3CloudStorageParametersV4ToS3FileSystemConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        S3FileSystem expected = new S3FileSystem();
        expected.setInstanceProfile(TEST_INSTANCE_PROFILE);

        S3FileSystem result = underTest.convert(createS3CloudStorageParameters());

        assertEquals(expected, result);
    }

    private S3CloudStorageParametersV4 createS3CloudStorageParameters() {
        S3CloudStorageParametersV4 s3 = new S3CloudStorageParametersV4();
        s3.setInstanceProfile(TEST_INSTANCE_PROFILE);
        return s3;
    }

}