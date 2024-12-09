package com.sequenceiq.cloudbreak.job.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component
public class DuplicatedSecretMigrationJobInitializer extends AbstractStackJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatedSecretMigrationJobInitializer.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private DuplicatedSecretMigrationJobService jobService;

    @Inject
    private DuplicatedSecretMigrationJobConfig config;

    @Override
    public void initJobs() {
        if (config.isDuplicatedSecretMigrationEnabled()) {
            clusterService.getClustersWithEmptyClusterManagerUser()
                    .forEach(c -> jobService.schedule(new DuplicatedSecretMigrationJobAdapter(c)));
        }
    }
}
