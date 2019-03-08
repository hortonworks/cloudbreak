package com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;

public final class ClouderaManagerRepoToClouderaManagerRepositoryV4Response {

    private ClouderaManagerRepoToClouderaManagerRepositoryV4Response() {
    }

    public static ClouderaManagerRepositoryV4Response convert(ClouderaManagerRepo clouderaManagerRepo) {
        return new ClouderaManagerRepositoryV4Response()
                .withBaseUrl(clouderaManagerRepo.getBaseUrl())
                .withGpgKeyUrl(clouderaManagerRepo.getGpgKeyUrl())
                .withVersion(clouderaManagerRepo.getVersion());
    }
}
