package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.controller.json.PlatformVariantsJson;

@Component
public class JsonToPlatformVariantsConverter extends AbstractConversionServiceAwareConverter<PlatformVariantsJson, PlatformVariants> {

    @Override
    public PlatformVariants convert(PlatformVariantsJson source) {
        return new PlatformVariants(source.getPlatformToVariants(), source.getDefaultVariants());
    }
}
