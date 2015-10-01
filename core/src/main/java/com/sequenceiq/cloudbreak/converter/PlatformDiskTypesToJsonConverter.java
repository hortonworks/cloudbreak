package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.controller.json.PlatformDisksJson;

@Component
public class PlatformDiskTypesToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformDisks, PlatformDisksJson> {

    @Override
    public PlatformDisksJson convert(PlatformDisks source) {
        PlatformDisksJson json = new PlatformDisksJson();
        json.setDefaultDisks(source.getDefaultDisks());
        json.setDiskTypes(source.getDiskTypes());
        return json;
    }
}
