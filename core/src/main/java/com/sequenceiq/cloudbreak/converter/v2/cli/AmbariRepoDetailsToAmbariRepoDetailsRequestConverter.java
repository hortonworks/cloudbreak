package com.sequenceiq.cloudbreak.converter.v2.cli;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AmbariRepoDetailsToAmbariRepoDetailsRequestConverter
        extends AbstractConversionServiceAwareConverter<AmbariRepo, AmbariRepoDetailsJson> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRepoDetailsToAmbariRepoDetailsRequestConverter.class);

    @Override
    public AmbariRepoDetailsJson convert(AmbariRepo source) {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setBaseUrl(source.getBaseUrl());
        ambariRepoDetailsJson.setGpgKeyUrl(source.getGpgKeyUrl());
        ambariRepoDetailsJson.setVersion(source.getVersion());
        return ambariRepoDetailsJson;
    }

}
