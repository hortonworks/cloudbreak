package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class S3BackupConfigGenerator extends CloudBackupConfigGenerator<S3BackupConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BackupConfigGenerator.class);

    private static final String[] S3_SCHEME_PREFIXES = {"s3://", "s3a://", "s3n://"};

    @Override
    public String generateBackupLocation(String location, String clusterType, String clusterName, String clusterId) {
        S3BackupConfig s3BackupConfig = generateBackupConfig(location);
        String generatedS3Location = S3_SCHEME_PREFIXES[0] + Paths.get(s3BackupConfig.getBucket(),
                resolveBackupFolder(s3BackupConfig, clusterType, clusterName, clusterId));
        LOGGER.debug("The following S3 base folder location is generated: {} (from {})",
                generatedS3Location, location);
        return generatedS3Location;
    }

    private S3BackupConfig generateBackupConfig(String location) {
        if (StringUtils.isNotEmpty(location)) {
            String locationWithoutScheme = getLocationWithoutSchemePrefixes(location, S3_SCHEME_PREFIXES);
            String[] locationSplit = locationWithoutScheme.split("/", 2);
            String folderPrefix = locationSplit.length < 2 ? "" :  locationSplit[1];
            return new S3BackupConfig(folderPrefix, locationSplit[0]);
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for S3");
    }
}
