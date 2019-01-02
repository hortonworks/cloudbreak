package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AmbariRepoDetailsToAmbariRepositoryV4RequestConverter
        extends AbstractConversionServiceAwareConverter<AmbariRepo, AmbariRepositoryV4Request> {

    @Override
    public AmbariRepositoryV4Request convert(AmbariRepo source) {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setBaseUrl(source.getBaseUrl());
        ambariRepoDetailsJson.setGpgKeyUrl(source.getGpgKeyUrl());
        ambariRepoDetailsJson.setVersion(source.getVersion());
        return ambariRepoDetailsJson;
    }

}
