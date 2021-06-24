package com.sequenceiq.cloudbreak.converter;


import com.sequenceiq.cloudbreak.api.model.CustomConfigPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigProperty;

public class CustomConfigPropertyConverter {

    private CustomConfigPropertyConverter() {
    }

    public static CustomConfigProperty convertFrom(CustomConfigPropertyParameters source) {
        CustomConfigProperty customConfigProperty = new CustomConfigProperty();
        customConfigProperty.setConfigName(source.getConfigName());
        customConfigProperty.setConfigValue(source.getConfigValue());
        customConfigProperty.setRoleType(source.getRoleType());
        customConfigProperty.setServiceType(source.getServiceType());
        return customConfigProperty;
    }

    public static CustomConfigPropertyParameters convertTo(CustomConfigProperty source) {
        CustomConfigPropertyParameters response = new CustomConfigPropertyParameters();
        response.setConfigName(source.getConfigName());
        response.setConfigValue(source.getConfigValue());
        response.setRoleType(source.getRoleType());
        response.setServiceType(source.getServiceType());
        return response;
    }
}
