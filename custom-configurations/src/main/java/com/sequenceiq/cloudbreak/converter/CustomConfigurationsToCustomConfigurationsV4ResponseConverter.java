package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;

@Component
public class CustomConfigurationsToCustomConfigurationsV4ResponseConverter {

    @Inject
    private CustomConfigurationPropertyConverter customConfigurationPropertyConverter;

    public CustomConfigurationsV4Response convert(CustomConfigurations source) {
        CustomConfigurationsV4Response response = new CustomConfigurationsV4Response();
        response.setName(source.getName());
        response.setCreated(source.getCreated());
        response.setCrn(source.getCrn());
        response.setConfigurations(source.getConfigurations()
                .stream()
                .map(c -> customConfigurationPropertyConverter.convertToResponseJson(c))
                .collect(Collectors.toSet()));
        response.setAccount(source.getAccount());
        response.setRuntimeVersion(source.getRuntimeVersion());
        return response;
    }

}
