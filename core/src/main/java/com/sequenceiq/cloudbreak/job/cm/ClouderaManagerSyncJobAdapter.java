package com.sequenceiq.cloudbreak.job.cm;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class ClouderaManagerSyncJobAdapter extends JobResourceAdapter<Stack> {

    public ClouderaManagerSyncJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public ClouderaManagerSyncJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return ClouderaManagerSyncJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}