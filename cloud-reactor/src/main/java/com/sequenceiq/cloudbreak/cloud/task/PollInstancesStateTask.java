package com.sequenceiq.cloudbreak.cloud.task;

import javax.inject.Inject;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class PollInstancesStateTask extends PollTask<InstancesStatusResult> {

    private List<CloudInstance> instances;

    @Inject
    public PollInstancesStateTask(AuthenticatedContext authenticatedContext, CloudConnector connector, List<CloudInstance> instances) {
        super(connector, authenticatedContext);
        this.instances = instances;
    }

    @Override
    public InstancesStatusResult call() throws Exception {
        List<CloudVmInstanceStatus> instanceStatuses = getConnector().instances().check(getAuthenticatedContext(), instances);
        return new InstancesStatusResult(getAuthenticatedContext().getCloudContext(), instanceStatuses);
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
