package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSUFFICIENT_INSTANCE_CAPACITY;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.UNSUPPORTED;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class InstanceTypeRetryExceptionMatcher {

    private InstanceTypeRetryExceptionMatcher() {
    }

    public static boolean isInstanceTypeNotSupported(AwsServiceException e) {
        return e.awsErrorDetails().errorCode().equalsIgnoreCase(INSUFFICIENT_INSTANCE_CAPACITY) ||
                e.awsErrorDetails().errorCode().equalsIgnoreCase(UNSUPPORTED);
    }
}
