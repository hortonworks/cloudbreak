package com.sequenceiq.cloudbreak.structuredevent.job;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class StructuredSynchronizerJobAdapter extends JobResourceAdapter<Stack> {

    public StructuredSynchronizerJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public StructuredSynchronizerJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return StructuredSynchronizerJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
