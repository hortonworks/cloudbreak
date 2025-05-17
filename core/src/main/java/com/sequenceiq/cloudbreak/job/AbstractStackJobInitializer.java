package com.sequenceiq.cloudbreak.job;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractStackJobInitializer implements JobInitializer {

    @Inject
    private StackService stackService;

    protected List<JobResource> getAliveJobResources() {
        return getJobResourcesNotIn(Status.getUnschedulableStatuses());
    }

    protected List<JobResource> getJobResourcesNotIn(Set<Status> statusesNotIn) {
        return stackService.getAllWhereStatusNotIn(statusesNotIn);
    }
}
