package com.sequenceiq.thunderhead.grpc.service.auth;

/**
 *  This is a simplified access control for mock authorization.
 *    - NOT_IMPLEMENTED means that there is no specific rule implemented for the given user and right combination, so the default behavior should be applied.
 *    - APPROVED means that the user has the right to access the resource.
 *    - DENIED means that the user does not have the right to access the resource.
 */
public enum MockPermissionControl {
    NOT_IMPLEMENTED,
    APPROVED,
    DENIED
}
