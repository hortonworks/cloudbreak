package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class DynamicEntitlementRefreshJobAdapter extends JobResourceAdapter<Stack> {

    public DynamicEntitlementRefreshJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public DynamicEntitlementRefreshJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return DynamicEntitlementRefreshJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
