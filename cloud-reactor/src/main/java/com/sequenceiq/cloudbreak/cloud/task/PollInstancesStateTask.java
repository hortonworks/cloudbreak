package com.sequenceiq.cloudbreak.cloud.task;

import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class PollInstancesStateTask implements PollTask<InstancesStatusResult> {

    private CloudConnector connector;
    private AuthenticatedContext authenticatedContext;
    private List<CloudInstance> instances;

    @Inject
    public PollInstancesStateTask(AuthenticatedContext authenticatedContext, CloudConnector connector, List<CloudInstance> instances) {
        this.authenticatedContext = authenticatedContext;
        this.connector = connector;
        this.instances = instances;
    }

    @Override
    public InstancesStatusResult call() throws Exception {
        List<CloudVmInstanceStatus> instanceStatuses = connector.instances().check(authenticatedContext, instances);
        return new InstancesStatusResult(authenticatedContext.getCloudContext(), instanceStatuses);
    }

    @Override
    public boolean completed(InstancesStatusResult instancesStatusResult) {
        for (CloudVmInstanceStatus result : instancesStatusResult.getResults()) {
            if (result.getStatus().isTransient()) {
                return false;
            }
        }
        return true;
    }
}
