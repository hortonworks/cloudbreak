package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;

public enum AwsNetworkParam implements TemplateParam {
    VPC_ID("vpcId", true, String.class, Optional.<String>absent()),
    INTERNET_GATEWAY_ID("internetGatewayId", true, String.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private AwsNetworkParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
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
