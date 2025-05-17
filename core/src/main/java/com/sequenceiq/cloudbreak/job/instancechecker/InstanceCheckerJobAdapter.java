package com.sequenceiq.cloudbreak.job.instancechecker;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class InstanceCheckerJobAdapter extends JobResourceAdapter<Stack> {

    public InstanceCheckerJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public InstanceCheckerJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return InstanceCheckerJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
