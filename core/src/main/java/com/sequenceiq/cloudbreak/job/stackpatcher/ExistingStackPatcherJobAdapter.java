package com.sequenceiq.cloudbreak.job.stackpatcher;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class ExistingStackPatcherJobAdapter extends JobResourceAdapter<Stack> {

    public ExistingStackPatcherJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public ExistingStackPatcherJobAdapter(Stack resource) {
        super(resource);
    }

    @Override
    public String getLocalId() {
        return String.valueOf(getResource().getId());
    }

    @Override
    public String getRemoteResourceId() {
        return getResource().getResourceCrn();
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
