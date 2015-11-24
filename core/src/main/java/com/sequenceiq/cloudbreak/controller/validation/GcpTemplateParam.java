package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;

public enum GcpTemplateParam implements TemplateParam {

    INSTANCETYPE("gcpInstanceType", true, String.class,
            Optional.of("^(?:n1-standard-(?:[1248]|16)|n1-highmem-(?:[248]|16)|n1-highcpu-(?:[248]|16))$")),
    CONTAINERCOUNT("containerCount", false, Integer.class, Optional.<String>absent()),
    TYPE("volumeType", false, String.class, Optional.of("^pd-(?:ssd|standard)"));

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
