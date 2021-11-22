package com.sequenceiq.datalake.job;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

public class SdxClusterJobAdapter extends JobResourceAdapter<SdxCluster> {

    public SdxClusterJobAdapter(SdxCluster resource) {
        super(resource);
    }

    public SdxClusterJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    @Override
    public String getLocalId() {
        return getResource().getId().toString();
    }

    @Override
    public String getRemoteResourceId() {
        return getResource().getStackCrn();
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
