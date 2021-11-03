package com.sequenceiq.cloudbreak.job.existingstackfix;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class ExistingStackFixerJobAdapter extends JobResourceAdapter<Stack> {

    public ExistingStackFixerJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public ExistingStackFixerJobAdapter(Stack resource) {
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
        return ExistingStackFixerJob.class;
    }

    @Override
    public Class<? extends CrudRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
