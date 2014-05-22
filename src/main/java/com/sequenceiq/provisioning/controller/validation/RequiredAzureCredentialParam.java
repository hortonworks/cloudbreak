package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAzureCredentialParam implements TemplateParam {

    SUBSCRIPTION_ID("subscriptionId", String.class),
    JKS_PASSWORD("jksPassword", String.class);

    private final String paramName;
    private final Class clazz;

    private RequiredAzureCredentialParam(String paramName, Class clazz) {
        this.paramName = paramName;
        this.clazz = clazz;
    }

    @Override
    public String getName() {
        return paramName;
    }

    @Override
    public Class getClazz() {
        return clazz;
    }
}
