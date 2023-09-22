package com.sequenceiq.cloudbreak.job.metering.sync;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class MeteringSyncJobAdapter extends JobResourceAdapter<Stack> {

    public MeteringSyncJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public MeteringSyncJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return MeteringSyncJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
