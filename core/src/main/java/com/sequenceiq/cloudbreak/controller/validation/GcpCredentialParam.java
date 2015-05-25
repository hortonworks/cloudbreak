package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;

public enum GcpCredentialParam implements TemplateParam {

    SERVICE_ACCOUNT_ID("serviceAccountId", true, String.class, Optional.<String>absent()),
    SERVICE_ACCOUNT_PRIVATE_KEY("serviceAccountPrivateKey", true, String.class, Optional.<String>absent()),
    PROJECTID("projectId", true, String.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private GcpCredentialParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
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
