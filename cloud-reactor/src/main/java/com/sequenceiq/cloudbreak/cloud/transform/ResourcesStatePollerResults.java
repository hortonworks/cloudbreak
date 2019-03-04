package com.sequenceiq.cloudbreak.cloud.transform;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;

public class ResourcesStatePollerResults {

    private ResourcesStatePollerResults() {
    }

    public static ResourcesStatePollerResult build(CloudContext context, List<CloudResourceStatus> results) {
        CloudResourceStatus status = ResourceStatusLists.aggregate(results);
        return new ResourcesStatePollerResult(context, status.getStatus(), status.getStatusReason(), results);
    }

    public static LaunchStackResult transformToLaunchStackResult(LaunchStackRequest request, ResourcesStatePollerResult result) {
        return new LaunchStackResult(request, result.getResults());
    }

    public static UpscaleStackResult transformToUpscaleStackResult(ResourcesStatePollerResult result, UpscaleStackRequest<?> request) {
        return new UpscaleStackResult(request, result.getStatus(), result.getResults());
    }
}
