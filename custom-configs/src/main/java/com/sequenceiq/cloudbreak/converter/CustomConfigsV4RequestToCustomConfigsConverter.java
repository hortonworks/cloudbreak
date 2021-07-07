package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.requests.CustomConfigsV4Request;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;

@Component
public class CustomConfigsV4RequestToCustomConfigsConverter extends AbstractConversionServiceAwareConverter<CustomConfigsV4Request, CustomConfigs> {

    @Override
    public CustomConfigs convert(CustomConfigsV4Request source) {
        CustomConfigs customConfigs = new CustomConfigs();
        customConfigs.setName(source.getName());
        customConfigs.setConfigs(source.getConfigs().stream().map(CustomConfigPropertyConverter::convertFrom)
                .collect(Collectors.toSet()));
        customConfigs.setPlatformVersion(source.getPlatformVersion());
        return customConfigs;
    }
}
