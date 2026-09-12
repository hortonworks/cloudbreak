package com.sequenceiq.freeipa.job.stackpatcher;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackPatchType;
import com.sequenceiq.freeipa.repository.StackRepository;

public class ExistingStackPatcherJobAdapter extends JobResourceAdapter<Stack> {

    public static final String STACK_PATCH_TYPE_NAME = "stackPatchTypeName";

    private StackPatchType stackPatchType;

    public ExistingStackPatcherJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public ExistingStackPatcherJobAdapter(JobResource jobResource, StackPatchType stackPatchType) {
        super(jobResource);
        this.stackPatchType = stackPatchType;
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return ExistingStackPatcherJob.class;
    }

    @Override
    public Class<StackRepository> getRepositoryClassForResource() {
        return StackRepository.class;
    }

    public StackPatchType getStackPatchType() {
        return stackPatchType;
    }

    @Override
    public JobDataMap toJobDataMap() {
        JobDataMap jobDataMap = super.toJobDataMap();
        jobDataMap.put(STACK_PATCH_TYPE_NAME, stackPatchType.name());
        return jobDataMap;
    }
}
