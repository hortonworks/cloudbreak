package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AmbariRepoDetailsToAmbariRepositoryV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<RepositoryDetails, AmbariRepositoryV4Response> {

    @Override
    public AmbariRepositoryV4Response convert(RepositoryDetails source) {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setBaseUrl(source.getBaseurl());
        ambariRepoDetailsJson.setGpgKeyUrl(source.getGpgkey());
        return ambariRepoDetailsJson;
    }

}
