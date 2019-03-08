package com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;

public final class ClouderaManagerRepoToClouderaManagerRepositoryV4Request {

    private ClouderaManagerRepoToClouderaManagerRepositoryV4Request() {
    }

    public static ClouderaManagerRepositoryV4Request convert(ClouderaManagerRepo clouderaManagerRepo) {
        return new ClouderaManagerRepositoryV4Request()
                .withBaseUrl(clouderaManagerRepo.getBaseUrl())
                .withGpgKeyUrl(clouderaManagerRepo.getGpgKeyUrl())
                .withVersion(clouderaManagerRepo.getVersion());
    }
}
