package com.sequenceiq.cloudbreak.job;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractStackJobInitializer implements JobInitializer {

    @Inject
    private StackService stackService;

    protected List<JobResource> getAliveJobResources() {
        return getJobResourcesNotIn(Set.of(DELETE_COMPLETED, DELETE_IN_PROGRESS, DELETE_FAILED, CREATE_FAILED, CREATE_IN_PROGRESS));
    }

    protected List<JobResource> getJobResourcesNotIn(Set<Status> statusesNotIn) {
        return stackService.getAllAliveForAutoSync(statusesNotIn);
    }
}
