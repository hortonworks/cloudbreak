package com.sequenceiq.cloudbreak.job.nodestatus;

import org.quartz.Job;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class NodeStatusJobAdapter extends JobResourceAdapter<Stack> {

    public NodeStatusJobAdapter(Stack resource) {
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
        return NodeStatusCheckerJob.class;
    }

    @Override
    public Class<? extends CrudRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
