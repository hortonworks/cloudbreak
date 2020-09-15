package com.sequenceiq.freeipa.sync;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;

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
    public Class<? extends CrudRepository<Stack, Long>> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
