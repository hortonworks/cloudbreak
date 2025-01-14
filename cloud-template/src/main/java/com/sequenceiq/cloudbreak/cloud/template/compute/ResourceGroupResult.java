package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;
import java.util.concurrent.Future;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;

public class ResourceGroupResult {

    private final Group group;

    private final Future<ResourceRequestResult<List<CloudResourceStatus>>> futures;

    public ResourceGroupResult(Group group, Future<ResourceRequestResult<List<CloudResourceStatus>>> futures) {
        this.group = group;
        this.futures = futures;
    }

    public Group getGroup() {
        return group;
    }

    public Future<ResourceRequestResult<List<CloudResourceStatus>>> getFutures() {
        return futures;
    }

}
