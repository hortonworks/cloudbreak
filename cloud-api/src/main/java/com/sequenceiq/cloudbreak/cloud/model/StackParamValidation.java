package com.sequenceiq.cloudbreak.cloud.model;

import com.google.common.base.Optional;

public class StackParamValidation {

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    public StackParamValidation(String paramName, Boolean required, Class clazz, Optional<String> regex) {
        this.paramName = paramName;
        this.required = required;
        this.clazz = clazz;
        this.regex = regex;
    }

    public String getName() {
        return paramName;
    }

    public Class getClazz() {
        return clazz;
    }

    public Boolean getRequired() {
        return required;
    }

    public Optional<String> getRegex() {
        return regex;
    }

}
