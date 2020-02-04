package com.sequenceiq.cloudbreak.cloud.task;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Component(PollInstancesStateTask.NAME)
@Scope("prototype")
public class PollInstancesStateTask extends AbstractPollTask<InstancesStatusResult> {
    public static final String NAME = "pollInstancesStateTask";

    private final List<CloudInstance> instances;

    private final InstanceConnector instanceConnector;

    private final Set<InstanceStatus> completedStatuses;

    public PollInstancesStateTask(AuthenticatedContext authenticatedContext, InstanceConnector instanceConnector, List<CloudInstance> instances) {
        this(authenticatedContext, instanceConnector, instances, Sets.newHashSet());
    }

    public PollInstancesStateTask(AuthenticatedContext authenticatedContext, InstanceConnector instanceConnector, List<CloudInstance> instances,
            Set<InstanceStatus> completedStatuses) {
        super(authenticatedContext);
        this.instances = instances;
        this.instanceConnector = instanceConnector;
        this.completedStatuses = completedStatuses;
    }

    @Override
    protected InstancesStatusResult doCall() {
        List<CloudVmInstanceStatus> instanceStatuses = instanceConnector.check(getAuthenticatedContext(), instances);
        return new InstancesStatusResult(getAuthenticatedContext().getCloudContext(), instanceStatuses);
    }

    @Override
    public boolean completed(InstancesStatusResult instancesStatusResult) {
        for (CloudVmInstanceStatus result : instancesStatusResult.getResults()) {
            if (result.getStatus().isTransient() || (!completedStatuses.isEmpty() && !completedStatuses.contains(result.getStatus()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PollInstancesStateTask.class.getSimpleName() + "[", "]")
                .add("instances=" + instances)
                .toString();
    }
}
