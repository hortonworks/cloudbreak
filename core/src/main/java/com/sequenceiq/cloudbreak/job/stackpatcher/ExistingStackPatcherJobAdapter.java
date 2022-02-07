package com.sequenceiq.cloudbreak.job.stackpatcher;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class ExistingStackPatcherJobAdapter extends JobResourceAdapter<Stack> {

    public ExistingStackPatcherJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public ExistingStackPatcherJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return ExistingStackPatcherJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
