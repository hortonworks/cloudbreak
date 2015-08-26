package com.sequenceiq.cloudbreak.cloud.task;

import java.util.List;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class PollInstancesStateTask extends PollTask<InstancesStatusResult> {

    private List<CloudInstance> instances;
    private InstanceConnector instanceConnector;

    @Inject
    public PollInstancesStateTask(AuthenticatedContext authenticatedContext, InstanceConnector instanceConnector, List<CloudInstance> instances) {
        super(authenticatedContext);
        this.instances = instances;
        this.instanceConnector = instanceConnector;
    }

    @Override
    public InstancesStatusResult call() throws Exception {
        List<CloudVmInstanceStatus> instanceStatuses = instanceConnector.check(getAuthenticatedContext(), instances);
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
