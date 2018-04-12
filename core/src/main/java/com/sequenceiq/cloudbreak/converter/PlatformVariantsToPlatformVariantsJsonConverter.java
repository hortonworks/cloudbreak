package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;
import org.springframework.stereotype.Component;

@Component
public class PlatformVariantsToPlatformVariantsJsonConverter extends AbstractConversionServiceAwareConverter<PlatformVariants, PlatformVariantsJson> {

    @Override
    public PlatformVariantsJson convert(PlatformVariants source) {
        PlatformVariantsJson json = new PlatformVariantsJson();
        json.setPlatformToVariants(PlatformConverterUtil.convertPlatformMap(source.getPlatformToVariants()));
        json.setDefaultVariants(PlatformConverterUtil.convertDefaults(source.getDefaultVariants()));
        return json;
    }
}
