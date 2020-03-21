package com.sequenceiq.environment.environment.sync;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.statuschecker.model.JobResourceAdapter;

public class EnvironmentJobAdapter extends JobResourceAdapter<Environment> {

    public EnvironmentJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public EnvironmentJobAdapter(Environment resource) {
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
        return EnvironmentStatusCheckerJob.class;
    }

    @Override
    public Class<? extends CrudRepository<Environment, Long>> getRepositoryClassForResource() {
        return EnvironmentRepository.class;
    }
}
