package com.sequenceiq.datalake.job;

import java.util.Optional;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.datalake.entity.SdxCluster;

/**
 * Maps an {@link SdxCluster} to {@link JobResource}
 */
public final class SdxClusterJobResources {

    private SdxClusterJobResources() {
    }

    public static JobResource fromSdxCluster(SdxCluster cluster) {
        return new SdxClusterBackedJobResource(cluster);
    }

    private record SdxClusterBackedJobResource(SdxCluster cluster) implements JobResource {

        @Override
        public String getLocalId() {
            return String.valueOf(cluster.getId());
        }

        @Override
        public String getRemoteResourceId() {
            return cluster.getResourceCrn();
        }

        @Override
        public String getName() {
            return cluster.getName();
        }

        @Override
        public Optional<String> getProvider() {
            return Optional.empty();
        }
    }
}
