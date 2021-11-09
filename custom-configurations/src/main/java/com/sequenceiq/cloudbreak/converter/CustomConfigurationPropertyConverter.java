package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;

@Component
public class CustomConfigurationPropertyConverter {

    public CustomConfigurationProperty convertFromRequestJson(CustomConfigurationPropertyParameters source) {
        CustomConfigurationProperty customConfigurationProperty = new CustomConfigurationProperty();
        customConfigurationProperty.setName(source.getName());
        customConfigurationProperty.setSecretValue(source.getValue());
        customConfigurationProperty.setRoleType(source.getRoleType());
        customConfigurationProperty.setServiceType(source.getServiceType());
        return customConfigurationProperty;
    }

    public CustomConfigurationPropertyParameters convertToResponseJson(CustomConfigurationProperty source) {
        CustomConfigurationPropertyParameters response = new CustomConfigurationPropertyParameters();
        response.setName(source.getName());
        response.setValue(source.getValue());
        response.setRoleType(source.getRoleType());
        response.setServiceType(source.getServiceType());
        return response;
    }
}
