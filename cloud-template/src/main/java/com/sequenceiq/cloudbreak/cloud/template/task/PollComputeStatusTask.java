package com.sequenceiq.cloudbreak.cloud.template.task;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.task.AbstractPollTask;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

@Component(PollComputeStatusTask.NAME)
@Scope("prototype")
public class PollComputeStatusTask extends AbstractPollTask<List<CloudVmInstanceStatus>> {
    public static final String NAME = "pollComputeStatusTask";

    private final ComputeResourceBuilder<ResourceBuilderContext> builder;

    private final ResourceBuilderContext context;

    private final CloudInstance instance;

    public PollComputeStatusTask(AuthenticatedContext auth, ComputeResourceBuilder<ResourceBuilderContext> builder, ResourceBuilderContext context,
            CloudInstance instance) {
        super(auth);
        this.builder = builder;
        this.context = context;
        this.instance = instance;
    }

    @Override
    protected List<CloudVmInstanceStatus> doCall() {
        return builder.checkInstances(context, getAuthenticatedContext(), Collections.singletonList(instance));
    }

    @Override
    public boolean completed(List<CloudVmInstanceStatus> status) {
        for (CloudVmInstanceStatus result : status) {
            if (result.getStatus().isTransient()) {
                return false;
            }
        }
        return true;
    }

}
