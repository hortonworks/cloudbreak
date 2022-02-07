package com.sequenceiq.environment.environment.sync;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;

public class EnvironmentJobAdapter extends JobResourceAdapter<Environment> {

    public EnvironmentJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public EnvironmentJobAdapter(JobResource resource) {
        super(resource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return EnvironmentStatusCheckerJob.class;
    }

    @Override
    public Class<? extends JobResourceRepository<Environment, Long>> getRepositoryClassForResource() {
        return EnvironmentRepository.class;
    }
}
