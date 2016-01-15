package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;

@Component
public class PlatformDiskTypesToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformDisks, PlatformDisksJson> {

    @Override
    public PlatformDisksJson convert(PlatformDisks source) {
        PlatformDisksJson json = new PlatformDisksJson();
        json.setDefaultDisks(PlatformConverterUtil.convertDefaults(source.getDefaultDisks()));
        json.setDiskTypes(PlatformConverterUtil.convertPlatformMap(source.getDiskTypes()));
        return json;
    }


}
