package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ClouderaManagerProductV4RequestToClouderaManagerProductConverter
        extends AbstractConversionServiceAwareConverter<ClouderaManagerRepositoryV4Request, ClouderaManagerRepo> {

    @Override
    public ClouderaManagerRepo convert(ClouderaManagerRepositoryV4Request request) {
        return new ClouderaManagerRepo()
                .withBaseUrl(request.getBaseUrl())
                .withGpgKeyUrl(request.getGpgKeyUrl())
                .withVersion(request.getVersion());
    }
}