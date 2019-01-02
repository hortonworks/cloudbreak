package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AmbariRepositoryV4ResponseToAmbariRepoConverter extends AbstractConversionServiceAwareConverter<AmbariRepositoryV4Response, AmbariRepo> {

    @Override
    public AmbariRepo convert(AmbariRepositoryV4Response source) {
        var ambariRepo = new AmbariRepo();
        ambariRepo.setPredefined(Boolean.FALSE);
        ambariRepo.setVersion(source.getVersion());
        ambariRepo.setBaseUrl(source.getBaseUrl());
        ambariRepo.setGpgKeyUrl(source.getGpgKeyUrl());
        return ambariRepo;
    }
}