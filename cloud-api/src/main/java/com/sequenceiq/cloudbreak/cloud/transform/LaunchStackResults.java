package com.sequenceiq.cloudbreak.cloud.transform;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class LaunchStackResults {

    public static LaunchStackResult build(StackContext context, List<CloudResourceStatus> results) {
        CloudResourceStatus status = ResourceStatusLists.aggregate(results);
        return new LaunchStackResult(context, status.getStatus(), status.getStatusReason(), results);
    }

}
