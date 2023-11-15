package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class S3ExpressBucketNameValidatorTest {

    @Test
    void testIsS3ExpressBucket() {
        assertTrue(S3ExpressBucketNameValidator.isS3ExpressBucket("test--x-s3"));
    }

    @Test
    void testIsS3ExpressBucketReturnsFalse() {
        assertFalse(S3ExpressBucketNameValidator.isS3ExpressBucket("test"));
    }
}
