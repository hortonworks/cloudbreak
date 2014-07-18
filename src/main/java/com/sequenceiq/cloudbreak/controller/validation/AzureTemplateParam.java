package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.AzureVmType;

public enum AzureTemplateParam implements TemplateParam {

    LOCATION("location", true, AzureLocation.class, Optional.<String>absent()),
    IMAGENAME("imageName", true, String.class, Optional.<String>absent()),
    VMTYPE("vmType", true, AzureVmType.class, Optional.<String>absent()),

    PORTS("ports", false, ArrayList.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private AzureTemplateParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
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
