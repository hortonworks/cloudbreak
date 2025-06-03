package com.sequenceiq.cloudbreak.cloud.aws.common;

public class AwsSdkErrorCodes {

    public static final String INSTANCE_NOT_FOUND = "InvalidInstanceID.NotFound";

    public static final String INSUFFICIENT_INSTANCE_CAPACITY = "InsufficientInstanceCapacity";

    public static final String ACCESS_DENIED = "AccessDenied";

    public static final String INVALID_INSTANCE_ID_MALFORMED = "InvalidInstanceID.Malformed";

    public static final String VALIDATION_ERROR = "ValidationError";

    public static final String AUTH_FAILURE = "AuthFailure";

    public static final String UNAUTHORIZED_OPERATION = "UnauthorizedOperation";

    public static final String OPT_IN_REQUIRED = "OptInRequired";

    public static final String VOLUME_NOT_FOUND = "InvalidVolume.NotFound";

    public static final String NOT_FOUND = "NotFound";

    public static final String LISTENER_NOT_FOUND = "ListenerNotFound";

    public static final String LOAD_BALANCER_NOT_FOUND = "LoadBalancerNotFound";

    public static final String GROUP_DUPLICATE = "InvalidGroup.Duplicate";

    public static final String DUPLICATE_LOAD_BALANCER_NAME = "DuplicateLoadBalancerName";

    public static final String DUPLICATE_TARGET_GROUP_NAME = "DuplicateTargetGroupName";

    public static final String DUPLICATE_LISTENER = "DuplicateListener";

    private AwsSdkErrorCodes() {

    }
}
