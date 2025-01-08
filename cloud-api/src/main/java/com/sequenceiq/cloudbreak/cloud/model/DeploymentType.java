package com.sequenceiq.cloudbreak.cloud.model;

import org.apache.commons.lang3.StringUtils;

public enum DeploymentType {

    PROVISION,
    CANARY_TEST_DEPLOYMENT;

    public static DeploymentType safeValueOf(String deploymentTypeString) {
        try {
            return StringUtils.isNotBlank(deploymentTypeString) ? valueOf(deploymentTypeString.trim()) : PROVISION;
        } catch (IllegalArgumentException ex) {
            return PROVISION;
        }
    }
}