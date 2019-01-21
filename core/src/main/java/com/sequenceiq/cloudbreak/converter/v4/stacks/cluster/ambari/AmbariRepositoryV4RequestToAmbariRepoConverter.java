package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

public class AmbariRepositoryV4RequestToAmbariRepoConverter extends AbstractConversionServiceAwareConverter<AmbariRepositoryV4Request, AmbariRepo> {

    @Override
    public AmbariRepo convert(AmbariRepositoryV4Request source) {
        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setPredefined(Boolean.FALSE);
        ambariRepo.setVersion(source.getVersion());
        ambariRepo.setBaseUrl(source.getBaseUrl());
        ambariRepo.setGpgKeyUrl(source.getGpgKeyUrl());
        return ambariRepo;
    }
}