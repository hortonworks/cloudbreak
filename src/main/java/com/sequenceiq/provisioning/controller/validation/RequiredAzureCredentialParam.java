package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAzureCredentialParam implements TemplateParam {

    SUBSCRIPTION_ID("subscriptionId", true, String.class),
    JKS_PASSWORD("jksPassword", true, String.class);

    private final String paramName;
    private final Class clazz;
    private final boolean required;

    private RequiredAzureCredentialParam(String paramName, Boolean required, Class clazz) {
        this.paramName = paramName;
        this.clazz = clazz;
        this.required = required;
    }

    @Override
    public String getName() {
        return paramName;
    }

    @Override
    public Class getClazz() {
        return clazz;
    }

    @Override
    public Boolean getRequired() {
        return required;
    }
}
