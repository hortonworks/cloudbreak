package com.sequenceiq.consumption.util;

import javax.validation.ValidationException;

import org.springframework.stereotype.Service;

import com.sequenceiq.common.model.FileSystemType;

@Service
public class CloudStorageLocationUtil {

    public void validateCloudStorageType(FileSystemType requiredType, String storageLocation) {
        if (storageLocation == null || !storageLocation.startsWith(requiredType.getProtocol())) {
            throw new ValidationException(String.format("Storage location must start with '%s' if required file system type is '%s'!",
                    requiredType.getProtocol(), requiredType.name()));
        }
    }

    public String getS3BucketName(String storageLocation) {
        validateCloudStorageType(FileSystemType.S3, storageLocation);
        storageLocation = storageLocation.replace(FileSystemType.S3.getProtocol() + "://", "");
        return storageLocation.split("/")[0];
    }
}
