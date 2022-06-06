package com.sequenceiq.consumption.util;

import javax.validation.ValidationException;

import com.sequenceiq.common.model.FileSystemType;

public class CloudStorageLocationUtil {

    private CloudStorageLocationUtil() {
    }

    public static void validateCloudStorageType(FileSystemType requiredType, String storageLocation) {
        if (storageLocation == null || !storageLocation.startsWith(requiredType.getProtocol())) {
            throw new ValidationException(String.format("Storage location must start with '%s' if required file system type is '%s'!",
                    requiredType.getProtocol(), requiredType.name()));
        }
    }

    public static String getS3BucketName(String storageLocation) {
        validateCloudStorageType(FileSystemType.S3, storageLocation);
        storageLocation = storageLocation.replace(FileSystemType.S3.getProtocol() + "://", "");
        return storageLocation.split("/")[0];
    }
}
