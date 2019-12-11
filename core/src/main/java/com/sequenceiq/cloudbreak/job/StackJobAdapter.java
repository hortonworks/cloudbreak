package com.sequenceiq.cloudbreak.job;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.statuschecker.model.JobResourceAdapter;

public class StackJobAdapter extends JobResourceAdapter<Stack> {

    public StackJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public StackJobAdapter(Stack resource) {
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
        return StackStatusCheckerJob.class;
    }

    @Override
    public Class<? extends CrudRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
