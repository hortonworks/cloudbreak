package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class GCSBackupConfigGenerator extends CloudBackupConfigGenerator<GCSBackupConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GCSBackupConfigGenerator.class);

    private static final String[] GCS_SCHEME_PREFIXES = {"gs://"};

    @Override
    public String generateBackupLocation(String location, String clusterType, String clusterName, String clusterId) {
        GCSBackupConfig gcsBackupConfig = generateBackupConfig(location);
        String generatedGcsLocation = GCS_SCHEME_PREFIXES[0] + Paths.get(gcsBackupConfig.getBucket(),
                resolveBackupFolder(gcsBackupConfig, clusterType, clusterName, clusterId));
        LOGGER.debug("The following GCS base folder location is generated: {} (from {})",
                generatedGcsLocation, location);
        return generatedGcsLocation;
    }

    private GCSBackupConfig generateBackupConfig(String location) {
        if (StringUtils.isNotEmpty(location)) {
            String locationWithoutScheme = getLocationWithoutSchemePrefixes(location, GCS_SCHEME_PREFIXES);
            String[] locationSplit = locationWithoutScheme.split("/", 2);
            String folderPrefix = locationSplit.length < 2 ? "" :  locationSplit[1];
            return new GCSBackupConfig(folderPrefix, locationSplit[0]);
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for GCS");
    }
}
