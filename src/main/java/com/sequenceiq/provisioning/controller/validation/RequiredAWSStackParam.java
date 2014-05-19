package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAWSStackParam {

    KEY_NAME("keyName"),
    REGION("region");

    private final String paramName;

    private RequiredAWSStackParam(String paramName) {
        this.paramName = paramName;
    }

    public String getName() {
        return paramName;
    }

    // TODO: add other required params

}
