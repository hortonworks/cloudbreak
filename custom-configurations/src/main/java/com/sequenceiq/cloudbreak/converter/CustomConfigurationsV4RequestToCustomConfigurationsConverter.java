package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;

@Component
public class CustomConfigurationsV4RequestToCustomConfigurationsConverter {

    public CustomConfigurations convert(CustomConfigurationsV4Request source) {
        CustomConfigurations customConfigurations = new CustomConfigurations();
        customConfigurations.setName(source.getName());
        customConfigurations.setConfigurations(source.getConfigurations().stream().map(CustomConfigurationPropertyConverter::convertFrom)
                .collect(Collectors.toSet()));
        customConfigurations.setRuntimeVersion(source.getRuntimeVersion());
        return customConfigurations;
    }
}
