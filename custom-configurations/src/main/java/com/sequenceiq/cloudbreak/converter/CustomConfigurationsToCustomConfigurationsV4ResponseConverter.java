package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;

@Component
public class CustomConfigurationsToCustomConfigurationsV4ResponseConverter extends
        AbstractConversionServiceAwareConverter<CustomConfigurations, CustomConfigurationsV4Response> {

    @Override
    public CustomConfigurationsV4Response convert(CustomConfigurations source) {
        CustomConfigurationsV4Response response = new CustomConfigurationsV4Response();
        response.setName(source.getName());
        response.setCreated(source.getCreated());
        response.setCrn(source.getCrn());
        response.setConfigurations(source.getConfigurations().stream().map(CustomConfigurationPropertyConverter::convertTo).collect(Collectors.toSet()));
        response.setAccount(source.getAccount());
        response.setRuntimeVersion(source.getRuntimeVersion());
        return response;
    }

}
