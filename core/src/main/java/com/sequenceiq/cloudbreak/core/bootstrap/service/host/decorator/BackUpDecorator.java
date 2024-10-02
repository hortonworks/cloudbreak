package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class BackUpDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackUpDecorator.class);

    private static final String[] S3_SCHEME_PREFIXES = {"s3://", "s3a://", "s3n://"};

    private static final String CLUSTER_BACKUP_PREFIX = "cluster-backups";

    public void decoratePillarWithBackup(StackDto stackDto, DetailedEnvironmentResponse environmentResponse,
            Map<String, SaltPillarProperties> servicePillar) {
        servicePillar.put("cdpluksvolumebackup", new SaltPillarProperties("/cdpluksvolumebackup/init.sls",
                singletonMap("cdpluksvolumebackup", getCdpLuksVolumeBackUpProperties(environmentResponse, stackDto))));
    }

    private String generateBackupLocation(String location, String clusterType, String clusterName, String clusterId) {
        S3BackupConfig s3BackupConfig = generateBackupConfig(location);
        String generatedS3Location = S3_SCHEME_PREFIXES[0] + Paths.get(s3BackupConfig.bucket,
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
            return new S3BackupConfig(locationSplit[0], folderPrefix);
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for S3");
    }

    private Map<String, Object> getCdpLuksVolumeBackUpProperties(DetailedEnvironmentResponse environmentResponse, StackDto stack) {
        String backUpLocation = getBackupLocation(environmentResponse);
        return Map.of(
                "backup_location", generateBackupLocation(backUpLocation, stack.getStack().getType().getResourceType(), stack.getName(),
                        Crn.fromString(stack.getResourceCrn()).getResource()),
                "aws_region", stack.getRegion());
    }

    private String getBackupLocation(DetailedEnvironmentResponse detailedEnvironmentResponse) {
        TelemetryResponse telemetryResponse = detailedEnvironmentResponse.getTelemetry();
        if (StringUtils.isNotEmpty(detailedEnvironmentResponse.getBackupLocation())) {
            LOGGER.info("Using the backup location");
            return detailedEnvironmentResponse.getBackupLocation();
        } else if (telemetryResponse != null && telemetryResponse.getLogging() != null &&
                StringUtils.isNotEmpty(telemetryResponse.getLogging().getStorageLocation())) {
            LOGGER.info("Backup location not configured. Using the log location");
            return telemetryResponse.getLogging().getStorageLocation();
        } else {
            LOGGER.error("Could not identify the location to store the backup");
            throw new BadRequestException("Backup Location is empty");
        }
    }

    private String getLocationWithoutSchemePrefixes(String input, String... schemePrefixes) {
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

    private String resolveBackupFolder(S3BackupConfig cloudBackupStorageConfig, String clusterType,
            String clusterName, String clusterId) {
        String clusterIdentifier = String.format("%s_%s", clusterName, clusterId);

        if (StringUtils.isNotEmpty(cloudBackupStorageConfig.folderPrefix())) {
            return Paths.get(cloudBackupStorageConfig.folderPrefix(), CLUSTER_BACKUP_PREFIX, clusterType,
                    clusterIdentifier).toString();
        } else {
            return Paths.get(CLUSTER_BACKUP_PREFIX, clusterType, clusterIdentifier).toString();
        }
    }

    private record S3BackupConfig(String bucket, String folderPrefix) { }
}
