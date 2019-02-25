package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class RepoDetailsToClouderaManagerRepositoryV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<RepositoryDetails, ClouderaManagerRepositoryV4Response> {

    @Override
    public ClouderaManagerRepositoryV4Response convert(RepositoryDetails source) {
        ClouderaManagerRepositoryV4Response repoDetailsJson = new ClouderaManagerRepositoryV4Response();
        repoDetailsJson.setBaseUrl(source.getBaseurl());
        repoDetailsJson.setGpgKeyUrl(source.getGpgkey());
        return repoDetailsJson;
    }

}
