package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

public class AmbariRepoToAmbariRepositoryV4ResponseConverter extends AbstractConversionServiceAwareConverter<AmbariRepo, AmbariRepositoryV4Response> {

    @Override
    public AmbariRepositoryV4Response convert(AmbariRepo source) {
        var response = new AmbariRepositoryV4Response();
        response.setBaseUrl(source.getBaseUrl());
        response.setGpgKeyUrl(source.getGpgKeyUrl());
        response.setVersion(source.getVersion());
        return response;
    }

}