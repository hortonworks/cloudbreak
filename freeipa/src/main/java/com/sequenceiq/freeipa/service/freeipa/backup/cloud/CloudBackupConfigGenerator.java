package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

public abstract class CloudBackupConfigGenerator<T extends CloudBackupStorageConfig> {

    protected static final String CLUSTER_BACKUP_PREFIX = "cluster-backups";

    public abstract String generateBackupLocation(String location, String clusterType,
            String clusterName, String clusterId);

    String getLocationWithoutSchemePrefixes(String input, String... schemePrefixes) {
        for (String schemePrefix : schemePrefixes) {
            if (input.startsWith(schemePrefix)) {
                String[] splitted = input.split(schemePrefix);
                if (splitted.length > 1) {
                    return splitted[1];
                }
            }
        }
        return input;
    }

    String resolveBackupFolder(CloudBackupStorageConfig cloudBackupStorageConfig, String clusterType,
            String clusterName, String clusterId) {
        String clusterIdentifier = String.format("%s_%s", clusterName, clusterId);

        if (StringUtils.isNotEmpty(cloudBackupStorageConfig.getFolderPrefix())) {
            return Paths.get(cloudBackupStorageConfig.getFolderPrefix(), CLUSTER_BACKUP_PREFIX, clusterType,
                    clusterIdentifier).toString();
        } else {
            return Paths.get(CLUSTER_BACKUP_PREFIX, clusterType, clusterIdentifier).toString();
        }
    }
}
