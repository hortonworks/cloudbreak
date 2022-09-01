package com.sequenceiq.datalake.job.salt;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

public class SdxSaltStatusCheckerJobAdapter extends JobResourceAdapter<SdxCluster> {
    public SdxSaltStatusCheckerJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public SdxSaltStatusCheckerJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return SdxSaltStatusCheckerJob.class;
    }

    @Override
    public Class<? extends JobResourceRepository<SdxCluster, Long>> getRepositoryClassForResource() {
        return SdxClusterRepository.class;
    }
}
