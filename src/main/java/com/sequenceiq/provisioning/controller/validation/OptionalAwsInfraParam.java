package com.sequenceiq.provisioning.controller.validation;

public enum OptionalAwsInfraParam {

    SSH_LOCATION("sshLocation");

    private final String paramName;

    private OptionalAwsInfraParam(String paramName) {
        this.paramName = paramName;
    }

    public String getName() {
        return paramName;
    }

}