package com.sequenceiq.provisioning.controller.json;

public enum RequiredAWSRequestParam {

    ROLE_ARN("roleArn"),
    KEY_NAME("keyName"),
    REGION("region");

    private final String paramName;

    private RequiredAWSRequestParam(String paramName) {
        this.paramName = paramName;
    }

    public String getName() {
        return paramName;
    }

    // TODO: add other required params

}
