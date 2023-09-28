package com.sequenceiq.environment.environment.v1.converter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.StorageLocationAware;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;

@Component
public class StorageLocationDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageLocationDecorator.class);

    private static final String PROTOCOL_SEPARATOR = "://";

    public void setBackupStorageLocationFromRequest(EnvironmentBackup backup, String storageLocation) {
        setStorageLocationFromRequestInternal(backup, storageLocation, "backup");
    }

    public void setLoggingStorageLocationFromRequest(EnvironmentLogging logging, String storageLocation) {
        setStorageLocationFromRequestInternal(logging, storageLocation, "logging");
    }

    private void setStorageLocationFromRequestInternal(StorageLocationAware storageLocationAware, String storageLocation, String locationType) {
        String effectiveStorageLocation = storageLocation;
        if (StringUtils.isNotBlank(effectiveStorageLocation) && !effectiveStorageLocation.contains(PROTOCOL_SEPARATOR)) {
            FileSystemType fileSystemType = null;
            if (storageLocationAware.getS3() != null) {
                fileSystemType = FileSystemType.S3;
            } else if (storageLocationAware.getGcs() != null) {
                fileSystemType = FileSystemType.GCS;
            } else if (storageLocationAware.getAdlsGen2() != null) {
                fileSystemType = FileSystemType.ADLS_GEN_2;
            } else {
                LOGGER.warn("Received input {} storage location without protocol: '{}' but the file system type could not be determined!", locationType,
                        storageLocation);
            }
            if (fileSystemType != null) {
                effectiveStorageLocation = fileSystemType.getProtocol() + PROTOCOL_SEPARATOR + effectiveStorageLocation;
                LOGGER.info("Input {} storage location without protocol: '{}'. Inferred location: '{}'.", locationType, storageLocation,
                        effectiveStorageLocation);
            }
        }
        storageLocationAware.setStorageLocation(effectiveStorageLocation);
    }

}
