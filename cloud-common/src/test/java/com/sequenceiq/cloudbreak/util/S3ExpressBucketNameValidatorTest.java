package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class S3ExpressBucketNameValidatorTest {

    private S3ExpressBucketNameValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new S3ExpressBucketNameValidator();
        ReflectionTestUtils.setField(underTest, "s3expressBucketSuffixPattern", "--x-s3");
    }

    @Test
    void testIsS3ExpressBucket() {
        assertTrue(underTest.isS3ExpressBucket("test--x-s3"));
    }

    @Test
    void testIsS3ExpressBucketReturnsFalse() {
        assertFalse(underTest.isS3ExpressBucket("test"));
    }
}
