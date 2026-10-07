package com.sequenceiq.stackpatcher.job;

import org.quartz.Job;
import org.quartz.JobDataMap;

import com.sequenceiq.cloudbreak.common.domain.StackPatchResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.stackpatcher.domain.StackPatchType;

public class ExistingStackPatcherJobAdapter extends JobResourceAdapter<StackPatchResource> {

    public static final String STACK_PATCH_TYPE_NAME = "stackPatchTypeName";

    private final StackPatchType stackPatchType;

    public ExistingStackPatcherJobAdapter(JobResource jobResource, StackPatchType stackPatchType) {
        super(jobResource);
        this.stackPatchType = stackPatchType;
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return ExistingStackPatcherJob.class;
    }

    @Override
    public Class<? extends JobResourceRepository<StackPatchResource, Long>> getRepositoryClassForResource() {
        throw new UnsupportedOperationException("Stack patcher job adapters are always constructed from an already loaded JobResource");
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
