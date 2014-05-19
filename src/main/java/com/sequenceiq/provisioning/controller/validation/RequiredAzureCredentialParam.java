package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAzureCredentialParam {

    SUBSCRIPTION_ID("subscriptionId"),
    JKS_PASSWORD("jksPassword");

    private final String paramName;

    private RequiredAzureCredentialParam(String paramName) {
        this.paramName = paramName;
    }

    public String getName() {
        return paramName;
    }
}
