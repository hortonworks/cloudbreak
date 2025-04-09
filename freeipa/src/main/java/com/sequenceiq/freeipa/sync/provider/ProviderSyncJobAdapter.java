package com.sequenceiq.freeipa.sync.provider;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

public class ProviderSyncJobAdapter extends JobResourceAdapter<Stack> {

    public ProviderSyncJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public ProviderSyncJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return ProviderSyncJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}