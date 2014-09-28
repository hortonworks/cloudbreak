package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccImageType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccInstanceType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccZone;

public enum GccTemplateParam implements TemplateParam {

    ZONE("gccZone", true, GccZone.class, Optional.<String>absent()),
    IMAGETYPE("gccImageType", true, GccImageType.class, Optional.<String>absent()),
    INSTANCETYPE("gccInstanceType", true, GccInstanceType.class, Optional.<String>absent()),
    CONTAINERCOUNT("containerCount", true, Integer.class, Optional.<String>absent());

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
