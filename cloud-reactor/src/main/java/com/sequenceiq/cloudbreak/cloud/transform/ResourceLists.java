package com.sequenceiq.cloudbreak.cloud.transform;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class ResourceLists {
    private ResourceLists() {
    }

    public static List<CloudResource> transform(List<CloudResourceStatus> cloudResourceStatuses) {
        return cloudResourceStatuses.stream().map(CloudResourceStatus::getCloudResource).collect(Collectors.toList());
    }
}
