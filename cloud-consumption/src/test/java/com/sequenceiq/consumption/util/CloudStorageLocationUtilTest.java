package com.sequenceiq.consumption.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.validation.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.common.model.FileSystemType;

public class CloudStorageLocationUtilTest {

    private static final String S3_OBJECT_PATH = "s3a://bucket-name/folder/file";

    private static final String ABFS_OBJECT_PATH = "abfs://FILESYSTEM@STORAGEACCOUNT.dfs.core.windows.net/PATH";

    private CloudStorageLocationUtil underTest;

    @BeforeEach
    void setUp() {
        underTest = new CloudStorageLocationUtil();
    }

    @Test
    public void testGetBucketName() {
        assertEquals("bucket-name", underTest.getS3BucketName(S3_OBJECT_PATH));
    }

    @Test
    public void testGetBucketNameWithInvalidStorageType() {
        assertThrows(ValidationException.class, () -> underTest.getS3BucketName(ABFS_OBJECT_PATH));
    }

    @ParameterizedTest(name = "With requiredType={0} and storageLocation={1}, validation should succeed: {2}")
    @MethodSource("scenarios")
    public void testValidateCloudStorageType(FileSystemType requiredType, String storageLocation, boolean valid) {
        if (valid) {
            assertDoesNotThrow(() -> underTest.validateCloudStorageType(requiredType, storageLocation));
        } else {
            assertThrows(ValidationException.class, () -> underTest.validateCloudStorageType(requiredType, storageLocation));
        }
    }

    static Object[][] scenarios() {
        return new Object[][]{
                {FileSystemType.S3,     S3_OBJECT_PATH,     true},
                {FileSystemType.S3,     ABFS_OBJECT_PATH,   false},
                {FileSystemType.S3,     "",                 false},
                {FileSystemType.S3,     null,               false},
                {FileSystemType.WASB,   S3_OBJECT_PATH,     false}
        };
    }
}
