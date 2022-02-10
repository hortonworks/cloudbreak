package com.sequenceiq.datalake.job;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

public class SdxClusterJobAdapter extends JobResourceAdapter<SdxCluster> {

    public SdxClusterJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    public SdxClusterJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return SdxClusterStatusCheckerJob.class;
    }

    @Override
    public Class<SdxClusterRepository> getRepositoryClassForResource() {
        return SdxClusterRepository.class;
    }
}
