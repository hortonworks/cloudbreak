package com.sequenceiq.cloudbreak.cloud.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.storage.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.common.type.filesystem.S3FileSystem;

public class S3CloudStorageParametersToS3FileSystemConverterTest {

    private static final String TEST_INSTANCE_PROFILE = "i-123123rfa23";

    private S3CloudStorageParametersToS3FileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new S3CloudStorageParametersToS3FileSystemConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        S3FileSystem expected = new S3FileSystem();
        expected.setInstanceProfile(TEST_INSTANCE_PROFILE);

        S3FileSystem result = underTest.convert(createS3CloudStorageParameters());

        assertEquals(expected, result);
    }

    private S3CloudStorageParameters createS3CloudStorageParameters() {
        S3CloudStorageParameters s3 = new S3CloudStorageParameters();
        s3.setInstanceProfile(TEST_INSTANCE_PROFILE);
        return s3;
    }

}