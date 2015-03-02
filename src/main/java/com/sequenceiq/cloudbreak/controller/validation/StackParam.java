package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

public enum StackParam implements TemplateParam {

    VPC_ID("vpcId", false, String.class, Optional.of("vpc-[a-z0-9]{8}"), StackParamGroup.CUSTOM_VPC),
    IGW_ID("internetGatewayId", false, String.class, Optional.of("igw-[a-z0-9]{8}"), StackParamGroup.CUSTOM_VPC),
    SUBNET_CIDR("subnetCIDR", false, String.class, Optional.of("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,2})"), StackParamGroup.CUSTOM_VPC);

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;
    private final StackParamGroup group;

    private StackParam(String paramName, Boolean required, Class clazz, Optional<String> regex, StackParamGroup group) {
        this.paramName = paramName;
        this.required = required;
        this.clazz = clazz;
        this.regex = regex;
        this.group = group;
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

    public StackParamGroup getGroup() {
        return group;
    }

    public static List<StackParam> getParamsByGroup(StackParamGroup stackParamGroup) {
        List<StackParam> params = new ArrayList<>();
        for (StackParam stackParam : StackParam.values()) {
            if (stackParamGroup.equals(stackParam.group)) {
                params.add(stackParam);
            }
        }
        return params;
    }

}
