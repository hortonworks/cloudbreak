package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

public abstract class CloudStorageConfigGenerator<T extends CloudStorageConfig> {

    private static final String CLUSTER_LOG_PREFIX = "cluster-logs";

    public abstract String generateStoredLocation(String location, String clusterType,
            String clusterName, String clusterId);

    public abstract T generateStorageConfig(String location);

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

    String resolveLogFolder(CloudStorageConfig cloudStorageConfig, String clusterType,
            String clusterName, String clusterId) {
        String folderPrefix = StringUtils.isNotEmpty(cloudStorageConfig.getFolderPrefix())
                ? Paths.get(cloudStorageConfig.getFolderPrefix(), CLUSTER_LOG_PREFIX).toString()
                : CLUSTER_LOG_PREFIX;
        return Paths.get(folderPrefix, clusterType, String.format("%s_%s", clusterName, clusterId)).toString();
    }
}
