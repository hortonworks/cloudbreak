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

    public static final String INCORRECT_STATE = "IncorrectState";

    public static final String INCORRECT_INSTANCE_STATE = "IncorrectInstanceState";

    public static final String INCORRECT_STATE_EXCEPTION = "IncorrectStateException";

    public static final String INVALID_HOST_STATE = "InvalidHostState";

    public static final String INVALID_STATE = "InvalidState";

    public static final String INTERNAL_FAILURE = "InternalFailure";

    public static final String INTERNAL_ERROR = "InternalError";

    public static final String INTERNAL_ERROR_EXCEPTION = "InternalErrorException";

    public static final String INTERNAL_SERVICE_ERROR = "InternalServiceError";

    public static final String REQUEST_EXPIRED = "RequestExpired";

    public static final String RESOURCE_IN_USE = "ResourceInUse";

    public static final String SERVER_INTERNAL = "ServerInternal";

    public static final String SERVICE_LINKED_ROLE_FAILURE = "ServiceLinkedRoleFailure";

    public static final String SERVICE_UNAVAILABLE = "ServiceUnavailable";

    public static final String VOLUME_IN_USE = "VolumeInUse";

    public static final String UNAVAILABLE = "Unavailable";

    private AwsSdkErrorCodes() {

    }
}
