package com.sequenceiq.cloudbreak.converter;


import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;

public class CustomConfigurationPropertyConverter {

    private CustomConfigurationPropertyConverter() {
    }

    public static CustomConfigurationProperty convertFrom(CustomConfigurationPropertyParameters source) {
        CustomConfigurationProperty customConfigurationProperty = new CustomConfigurationProperty();
        customConfigurationProperty.setName(source.getName());
        customConfigurationProperty.setValue(source.getValue());
        customConfigurationProperty.setRoleType(source.getRoleType());
        customConfigurationProperty.setServiceType(source.getServiceType());
        return customConfigurationProperty;
    }

    public static CustomConfigurationPropertyParameters convertTo(CustomConfigurationProperty source) {
        CustomConfigurationPropertyParameters response = new CustomConfigurationPropertyParameters();
        response.setName(source.getName());
        response.setValue(source.getValue());
        response.setRoleType(source.getRoleType());
        response.setServiceType(source.getServiceType());
        return response;
    }
}
