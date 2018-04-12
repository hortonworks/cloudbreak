package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;
import org.springframework.stereotype.Component;

@Component
public class PlatformOrchestratorsToPlatformOrchestratorsJsonConverter
        extends AbstractConversionServiceAwareConverter<PlatformOrchestrators, PlatformOrchestratorsJson> {

    @Override
    public PlatformOrchestratorsJson convert(PlatformOrchestrators source) {
        PlatformOrchestratorsJson json = new PlatformOrchestratorsJson();
        json.setOrchestrators(PlatformConverterUtil.convertPlatformMap(source.getOrchestrators()));
        json.setDefaults(PlatformConverterUtil.convertDefaults(source.getDefaults()));
        return json;
    }
}
