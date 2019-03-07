package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;

public final class ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter {

    private ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter() {
    }

    public static ClouderaManagerRepo convert(ClouderaManagerRepositoryV4Request request) {
        return new ClouderaManagerRepo()
                .withBaseUrl(request.getBaseUrl())
                .withGpgKeyUrl(request.getGpgKeyUrl())
                .withVersion(request.getVersion());
    }
}