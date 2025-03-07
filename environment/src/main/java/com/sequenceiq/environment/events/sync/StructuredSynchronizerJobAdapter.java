package com.sequenceiq.environment.events.sync;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;

public class StructuredSynchronizerJobAdapter extends JobResourceAdapter<Environment> {

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
    public Class<? extends JobResourceRepository<Environment, Long>> getRepositoryClassForResource() {
        return EnvironmentRepository.class;
    }
}
