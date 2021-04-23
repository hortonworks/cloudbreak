package com.sequenceiq.cloudbreak.cloud.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LocationHelperTest {

    private LocationHelper underTest = new LocationHelper();

    @ParameterizedTest
    @CsvSource({
            "s3a://bucket-name, bucket-name",
            "s3a://bucket-name/path1/path2, bucket-name",
            "s3://bucket-name/path1/path2, bucket-name",
            "s3b://bucket-name/path1/path2, bucket-name",
            "s3c://buck_et-na2.3me/path1/path2, buck_et-na2.3me",
    })
    void testExtractS3Bucket(String s3Location, String expectedBucketName) {
        assertEquals(expectedBucketName, underTest.parseS3BucketName(s3Location));
    }
}
