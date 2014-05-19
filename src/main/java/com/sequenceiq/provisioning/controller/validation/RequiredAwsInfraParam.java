package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAwsInfraParam {

    KEY_NAME("keyName"),
    REGION("region");

    private final String paramName;

    private RequiredAwsInfraParam(String paramName) {
        this.paramName = paramName;
    }

    public String getName() {
        return paramName;
    }

    // TODO: add other required params

}
