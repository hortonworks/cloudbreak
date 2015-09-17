package com.sequenceiq.cloudbreak.cloud.transform;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateStackResult;
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

    public static LaunchStackResult transformToLaunchStackResult(ResourcesStatePollerResult result) {
        return new LaunchStackResult(result.getCloudContext(), result.getResults());
    }

    public static UpscaleStackResult transformToUpscaleStackResult(ResourcesStatePollerResult result) {
        return new UpscaleStackResult(result.getCloudContext(), result.getStatus(), result.getStatusReason(), result.getResults());
    }

    public static UpdateStackResult transformToUpdateStackResult(ResourcesStatePollerResult result) {
        return new UpdateStackResult(result.getCloudContext(), result.getStatus(), result.getStatusReason(), result.getResults());
    }

}
