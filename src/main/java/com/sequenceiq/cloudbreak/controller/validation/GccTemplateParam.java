package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccInstanceType;

public enum GccTemplateParam implements TemplateParam {

    INSTANCETYPE("gccInstanceType", true, GccInstanceType.class, Optional.<String>absent()),
    CONTAINERCOUNT("containerCount", false, Integer.class, Optional.<String>absent()),
    TYPE("volumeType", false, GccInstanceType.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private GccTemplateParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
        this.paramName = paramName;
        this.required = required;
        this.clazz = clazz;
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
