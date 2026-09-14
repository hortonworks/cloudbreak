package com.sequenceiq.cloudbreak.cloud.aws.common;

public enum AssumeRoleType {
    // Default aws credential chain
    DEFAULT_CREDENTIAL_CHAIN,
    // Use delegator role as part of https://cloudera.atlassian.net/browse/CB-34210
    DELEGATOR;

    public static AssumeRoleType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
