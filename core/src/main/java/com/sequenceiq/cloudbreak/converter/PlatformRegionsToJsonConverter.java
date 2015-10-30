package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.controller.json.PlatformRegionsJson;

@Component
public class PlatformRegionsToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformRegions, PlatformRegionsJson> {

    @Override
    public PlatformRegionsJson convert(PlatformRegions source) {
        PlatformRegionsJson json = new PlatformRegionsJson();
        json.setAvailabiltyZones(source.getAvailabiltyZones());
        json.setDefaultRegions(source.getDefaultRegions());
        json.setRegions(source.getRegions());
        return json;
    }
}
