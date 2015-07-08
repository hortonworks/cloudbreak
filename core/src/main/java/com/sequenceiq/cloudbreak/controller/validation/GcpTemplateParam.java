package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.GcpInstanceType;
import com.sequenceiq.cloudbreak.domain.GcpRawDiskType;

public enum GcpTemplateParam implements TemplateParam {

    INSTANCETYPE("gcpInstanceType", true, GcpInstanceType.class, Optional.<String>absent()),
    CONTAINERCOUNT("containerCount", false, Integer.class, Optional.<String>absent()),
    TYPE("volumeType", false, GcpRawDiskType.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private GcpTemplateParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
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
