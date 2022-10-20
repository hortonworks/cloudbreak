package com.sequenceiq.cloudbreak.job.salt;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

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
    public Class<? extends JobResourceRepository<Stack, Long>> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
