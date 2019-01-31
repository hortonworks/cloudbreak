package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AmbariRepoDetailsToAmbariRepositoryV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<AmbariRepo, AmbariRepositoryV4Response> {

    @Override
    public AmbariRepositoryV4Response convert(AmbariRepo source) {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setBaseUrl(source.getBaseUrl());
        ambariRepoDetailsJson.setGpgKeyUrl(source.getGpgKeyUrl());
        ambariRepoDetailsJson.setVersion(source.getVersion());
        return ambariRepoDetailsJson;
    }

}
