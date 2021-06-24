package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.responses.CustomConfigsV4Response;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;

@Component
public class CustomConfigsToCustomConfigsV4ResponseConverter extends AbstractConversionServiceAwareConverter<CustomConfigs, CustomConfigsV4Response> {

    @Override
    public CustomConfigsV4Response convert(CustomConfigs source) {
        CustomConfigsV4Response response = new CustomConfigsV4Response();
        response.setName(source.getName());
        response.setCreated(source.getCreated());
        response.setResourceCrn(source.getResourceCrn());
        response.setLastModified(source.getLastModified());
        response.setConfigs(source.getConfigs().stream().map(CustomConfigPropertyConverter::convertTo).collect(Collectors.toSet()));
        response.setAccount(source.getAccount());
        response.setPlatformVersion(source.getPlatformVersion());
        return response;
    }

}
