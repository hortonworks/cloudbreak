package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.controller.json.PlatformVariantsJson;

@Component
public class PlatformVariantsToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformVariants, PlatformVariantsJson> {

    @Override
    public PlatformVariantsJson convert(PlatformVariants source) {
        PlatformVariantsJson json = new PlatformVariantsJson();
        json.setPlatformToVariants(source.getPlatformToVariants());
        json.setDefaultVariants(source.getDefaultVariants());
        return json;
    }
}
