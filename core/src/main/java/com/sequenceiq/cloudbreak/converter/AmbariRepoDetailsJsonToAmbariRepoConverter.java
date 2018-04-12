package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;

@Component
public class AmbariRepoDetailsJsonToAmbariRepoConverter extends AbstractConversionServiceAwareConverter<AmbariRepoDetailsJson, AmbariRepo> {

    @Override
    public AmbariRepo convert(AmbariRepoDetailsJson source) {
        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setPredefined(Boolean.FALSE);
        ambariRepo.setVersion(source.getVersion());
        ambariRepo.setBaseUrl(source.getBaseUrl());
        ambariRepo.setGpgKeyUrl(source.getGpgKeyUrl());
        return ambariRepo;
    }
}
