package com.sequenceiq.freeipa.sync;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

public class StackJobAdapter extends JobResourceAdapter<Stack> {

    public StackJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public StackJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return StackStatusCheckerJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
