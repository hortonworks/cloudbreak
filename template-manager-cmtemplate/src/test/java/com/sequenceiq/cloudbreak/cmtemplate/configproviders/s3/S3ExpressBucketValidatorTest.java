package com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

@ExtendWith(MockitoExtension.class)
class S3ExpressBucketValidatorTest {

    @Test
    void testValidateVersionForS3ExpressBucket() {
        ClouderaManagerProduct cmp = mock(ClouderaManagerProduct.class);
        doReturn("CDH").when(cmp).getName();
        doReturn("7.2.18").when(cmp).getVersion();
        assertTrue(S3ExpressBucketValidator.validateVersionForS3ExpressBucket(List.of(cmp)));
    }

    @Test
    void testValidateVersionForS3ExpressBucketReturnsFalse() {
        ClouderaManagerProduct cmp = mock(ClouderaManagerProduct.class);
        doReturn("CDH").when(cmp).getName();
        doReturn("7.2.17").when(cmp).getVersion();
        assertFalse(S3ExpressBucketValidator.validateVersionForS3ExpressBucket(List.of(cmp)));
    }
}
