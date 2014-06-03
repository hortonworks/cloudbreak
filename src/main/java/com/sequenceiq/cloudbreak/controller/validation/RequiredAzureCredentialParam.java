package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;

public enum RequiredAzureCredentialParam implements TemplateParam {

    SUBSCRIPTION_ID("subscriptionId", true, String.class, Optional.<String>absent()),
    JKS_PASSWORD("jksPassword", true, String.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private RequiredAzureCredentialParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
        this.paramName = paramName;
        this.clazz = clazz;
        this.required = required;
        this.regex = regex;
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

    @Override
    public Optional<String> getRegex() {
        return regex;
    }
}
