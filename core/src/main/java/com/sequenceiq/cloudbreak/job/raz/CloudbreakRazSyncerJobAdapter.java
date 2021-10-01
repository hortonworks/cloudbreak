package com.sequenceiq.cloudbreak.job.raz;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class CloudbreakRazSyncerJobAdapter extends JobResourceAdapter<Stack> {

    public CloudbreakRazSyncerJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public CloudbreakRazSyncerJobAdapter(Stack resource) {
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
        return CloudbreakRazSyncerJob.class;
    }

    @Override
    public Class<? extends CrudRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
