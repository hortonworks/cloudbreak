package com.sequenceiq.freeipa.sync.saltstatus;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

public class StackSaltStatusCheckerJobAdapter extends JobResourceAdapter<Stack> {

    public StackSaltStatusCheckerJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public StackSaltStatusCheckerJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return StackSaltStatusCheckerJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
