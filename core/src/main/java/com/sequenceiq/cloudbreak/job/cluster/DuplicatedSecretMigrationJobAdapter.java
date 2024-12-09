package com.sequenceiq.cloudbreak.job.cluster;

import org.quartz.Job;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;

public class DuplicatedSecretMigrationJobAdapter extends JobResourceAdapter<Cluster> {

    public DuplicatedSecretMigrationJobAdapter(Long id, ApplicationContext context) {
        super(id, context);
    }

    public DuplicatedSecretMigrationJobAdapter(JobResource jobResource) {
        super(jobResource);
    }

    @Override
    public Class<? extends Job> getJobClassForResource() {
        return DuplicatedSecretMigrationJob.class;
    }

    @Override
    public Class<ClusterRepository> getRepositoryClassForResource() {
        return ClusterRepository.class;
    }
}
