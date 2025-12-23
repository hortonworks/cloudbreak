package com.sequenceiq.cloudbreak.rotation;

public enum SecretTypeFlag {
    INTERNAL,
    POST_FLOW,
    SKIP_SALT_UPDATE,
    SKIP_SALT_HIGHSTATE,
    SKIP_STATUS_CHECK
}
