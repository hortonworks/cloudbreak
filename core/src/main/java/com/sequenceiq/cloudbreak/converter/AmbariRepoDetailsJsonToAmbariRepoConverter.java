package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;

@Component
public class AmbariRepoDetailsJsonToAmbariRepoConverter extends AbstractConversionServiceAwareConverter<AmbariRepoDetailsJson, AmbariRepo> {

    @Value("${cb.ambari.repo.version}")
    private String version;

    @Value("${cb.ambari.repo.baseurl}")
    private String baseUrl;

    @Value("${cb.ambari.repo.gpgkey}")
    private String gpgKeyUrl;

    @Override
    public AmbariRepo convert(AmbariRepoDetailsJson source) {
        AmbariRepo ambariRepo;
        if (source == null || source.getVersion() == null) {
            ambariRepo = getDefault();
        } else {
            ambariRepo = new AmbariRepo();
            ambariRepo.setPredefined(Boolean.FALSE);
            ambariRepo.setVersion(source.getVersion());
            ambariRepo.setBaseUrl(source.getBaseUrl());
            ambariRepo.setGpgKeyUrl(source.getGpgKeyUrl());
        }
        return ambariRepo;
    }

    private AmbariRepo getDefault() {
        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setPredefined(Boolean.FALSE);
        ambariRepo.setVersion(version);
        ambariRepo.setBaseUrl(baseUrl);
        ambariRepo.setGpgKeyUrl(gpgKeyUrl);
        return ambariRepo;
    }


}
