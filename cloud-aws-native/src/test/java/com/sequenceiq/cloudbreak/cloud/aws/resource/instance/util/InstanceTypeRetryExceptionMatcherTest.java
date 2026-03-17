package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSUFFICIENT_INSTANCE_CAPACITY;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

@ExtendWith(MockitoExtension.class)
class InstanceTypeRetryExceptionMatcherTest {

    @Test
    void testIsInstanceTypeNotSupported() {
        AwsServiceException exception = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(UNSUPPORTED)
                        .errorMessage("blah blah blah").build())
                .build();
        boolean result = InstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(exception);
        assertTrue(result);
    }

    @Test
    void testIsInstanceTypeNotSupportedCapacity() {
        AwsServiceException exception = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(INSUFFICIENT_INSTANCE_CAPACITY)
                        .errorMessage("blah blah blah").build())
                .build();
        boolean result = InstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(exception);
        assertTrue(result);
    }

    @Test
    void testIsInstanceTypeNotSupportedFalseErrorCode() {
        AwsServiceException exception = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(AwsSdkErrorCodes.INTERNAL_ERROR)
                        .errorMessage("Launch Failed Your requested instance type (t2.micro) is not supported in your requested" +
                                " Availability Zone (ap-south-1c). Please retry your request by not specifying an Availability" +
                                " Zone or choosing ap-south-1a, ap-south-1b.").build())
                .build();
        boolean result = InstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(exception);
        assertFalse(result);
    }
}