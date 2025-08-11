package com.sequenceiq.freeipa.sync.crossrealmtrust;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

public class CrossRealmTrustStatusSyncJobAdapter extends JobResourceAdapter<Stack> {

    public CrossRealmTrustStatusSyncJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public CrossRealmTrustStatusSyncJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return CrossRealmTrustStatusSyncJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}