package com.sequenceiq.cloudbreak.job.metering.instancechecker;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.StackRepository;

public class MeteringInstanceCheckerJobAdapter extends JobResourceAdapter<Stack> {

    public MeteringInstanceCheckerJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public MeteringInstanceCheckerJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return MeteringInstanceCheckerJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }
}
