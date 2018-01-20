package com.sequenceiq.cloudbreak.converter;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

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

    public AmbariRepo convert(Map<String, String> repoSource, String version, Boolean predefined) throws CloudbreakImageCatalogException {
        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setPredefined(predefined);
        ambariRepo.setVersion(version);
        if (repoSource != null && repoSource.size() == 1) {
            for (String baseUrl : repoSource.values()) {
                ambariRepo.setBaseUrl(baseUrl);
            }

        } else {
            throw new CloudbreakImageCatalogException(String.format("Invalid Ambari repo present in image catalog: '%s'.", repoSource));
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
