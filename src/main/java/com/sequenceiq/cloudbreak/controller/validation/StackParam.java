package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;

public enum StackParam implements TemplateParam {

    VPC_ID("vpcId", false, String.class, Optional.of("vpc-[a-z0-9]{8}")),
    IGW_ID("internetGatewayId", false, String.class, Optional.of("igw-[a-z0-9]{8}")),
    SUBNET_CIDR("subnetCIDR", false, String.class, Optional.of("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,2})"));

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private StackParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
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
