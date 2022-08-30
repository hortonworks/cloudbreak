package com.sequenceiq.cloudbreak.api.model;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;

public enum RotateSaltPasswordReason {
    UNSET,
    MANUAL,
    EXPIRED,
    UNAUTHORIZED;

    public static Optional<RotateSaltPasswordReason> getForStatus(SaltPasswordStatus status) {
        switch (status) {
            case OK:
            case FAILED_TO_CHECK:
                return Optional.empty();
            case EXPIRES:
                return Optional.of(RotateSaltPasswordReason.EXPIRED);
            case INVALID:
                return Optional.of(RotateSaltPasswordReason.UNAUTHORIZED);
            default:
                throw new IllegalStateException("SaltPasswordStatus not handled: " + status);
        }
    }
}
