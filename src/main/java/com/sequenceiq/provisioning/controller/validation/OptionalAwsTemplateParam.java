package com.sequenceiq.provisioning.controller.validation;

public enum OptionalAwsTemplateParam {

    SSH_LOCATION("sshLocation");

    private final String paramName;

    private OptionalAwsTemplateParam(String paramName) {
        this.paramName = paramName;
    }

    public String getName() {
        return paramName;
    }

}