package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.freeipa.api.model.Backup;

@Service
public class CloudBackupFolderResolverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudBackupFolderResolverService.class);

    private final S3BackupConfigGenerator s3BackupConfigGenerator;

    private final AdlsGen2BackupConfigGenerator adlsGen2BackupConfigGenerator;

    public CloudBackupFolderResolverService(S3BackupConfigGenerator s3BackupConfigGenerator,
            AdlsGen2BackupConfigGenerator adlsGen2BackupConfigGenerator) {
        this.s3BackupConfigGenerator = s3BackupConfigGenerator;
        this.adlsGen2BackupConfigGenerator = adlsGen2BackupConfigGenerator;
    }

    public void updateStorageLocation(Backup backup, String clusterType,
            String clusterName, String clusterCrn) {
        LOGGER.debug("Updating/enriching backup storage locations with cluster data.");
        if (backup != null  && StringUtils.isNotEmpty(backup.getStorageLocation())) {
            String storageLocation = backup.getStorageLocation();
            if (backup.getS3() != null) {
                storageLocation = resolveS3Location(storageLocation,
                        clusterType, clusterName, clusterCrn);
            } else if (backup.getAdlsGen2() != null) {
                storageLocation = resolveAdlsGen2Location(storageLocation,
                        clusterType, clusterName, clusterCrn);
            } else {
                LOGGER.warn("None of the backup storage location was resolved, "
                        + "make sure storage type is set properly (currently supported: s3, abfs)");
            }
            backup.setStorageLocation(storageLocation);
        } else {
            LOGGER.debug("Backup is not set, skipping cloud storage location updates.");
        }
    }

    public String resolveS3Location(String location, String clusterType,
            String clusterName, String clusterCrn) {
        LOGGER.debug("Start to resolve S3 storage location for telemetry (logging).");
        return s3BackupConfigGenerator.generateBackupLocation(location,
                clusterType, clusterName, Crn.fromString(clusterCrn).getResource());
    }

    public String resolveAdlsGen2Location(String location, String clusterType,
            String clusterName, String clusterCrn) {
        LOGGER.debug("Start to resolve ADLS V2 storage location for telemetry (logging).");
        return adlsGen2BackupConfigGenerator.generateBackupLocation(location,
                clusterType, clusterName, Crn.fromString(clusterCrn).getResource());
    }
}
