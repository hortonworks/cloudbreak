package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class GcsConfigGenerator extends CloudStorageConfigGenerator<GcsConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcsConfigGenerator.class);

    private static final String[] GCS_SCHEME_PREFIXES = {"gs://"};

    @Override
    public String generateStoredLocation(String location, String clusterType, String clusterName, String clusterId) {
        GcsConfig gcsConfig = generateStorageConfig(location);
        String generatedGcsLocation = GCS_SCHEME_PREFIXES[0] + Paths.get(gcsConfig.getBucket(),
                resolveLogFolder(gcsConfig, clusterType, clusterName, clusterId));
        LOGGER.debug("The following GCS base folder location is generated: {} (from {})",
                generatedGcsLocation, location);
        return generatedGcsLocation;
    }

    @Override
    public GcsConfig generateStorageConfig(String location) {
        if (StringUtils.isNotEmpty(location)) {
            String locationWithoutScheme = getLocationWithoutSchemePrefixes(location, GCS_SCHEME_PREFIXES);
            String[] splitted = locationWithoutScheme.split("/", 2);
            String folderPrefix = splitted.length < 2 ? "" :  splitted[1];
            return new GcsConfig(folderPrefix, splitted[0], null);
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for GCS");
    }

    public GcsConfig generateStorageConfig(String location, String serviceAccountEmail) {
        GcsConfig configFromLocation = generateStorageConfig(location);
        if (StringUtils.isNotBlank(serviceAccountEmail)) {
            String[] splitted = serviceAccountEmail.split("@");
            if (splitted.length > 1) {
                String[] splitByDot = splitted[1].split("\\.");
                String projectId = splitByDot[0];
                LOGGER.debug("Project ID for GCS logging: {}", projectId);
                return new GcsConfig(configFromLocation.getFolderPrefix(), configFromLocation.getBucket(), projectId);
            } else {
                throw new CloudbreakServiceException(
                        String.format("Service account email parameter (GCS) is not valid: %s", serviceAccountEmail));
            }
        }
        throw new CloudbreakServiceException("Service account email parameter is missing or empty for GCS");
    }

}
