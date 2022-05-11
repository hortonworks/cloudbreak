package com.sequenceiq.datalake.job;

import org.quartz.Job;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

public class SdxRollForwardJobAdapter extends JobResourceAdapter<SdxCluster> {

    public SdxRollForwardJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return SdxRollForwardJob.class;
    }

    @Override
    public Class<? extends JobResourceRepository<SdxCluster, Long>> getRepositoryClassForResource() {
        return SdxClusterRepository.class;
    }
}
