package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;

@Component
public class AmbariRepoToAmbariRepoDetailsJsonConverter extends AbstractConversionServiceAwareConverter<AmbariRepo, AmbariRepoDetailsJson> {

    @Override
    public AmbariRepoDetailsJson convert(AmbariRepo source) {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion(source.getVersion());
        ambariRepoDetailsJson.setBaseUrl(source.getBaseUrl());
        ambariRepoDetailsJson.setGpgKeyUrl(source.getGpgKeyUrl());
        return ambariRepoDetailsJson;
    }
}
