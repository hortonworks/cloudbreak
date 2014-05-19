package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAWSCredentialParam {

    ROLE_ARN("roleArn");

    private final String paramName;

    private RequiredAWSCredentialParam(String paramName) {
        this.paramName = paramName;
    }

    public String getName() {
        return paramName;
    }
}
