package com.sequenceiq.cloudbreak.service.filesystem;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.FileSystemType;

@Service
public class FileSystemResolver {

    public FileSystemType determineFileSystemType(CloudStorageBase cloudStorageRequest) {

        FileSystemType fileSystemType = null;
        if (cloudStorageRequest != null) {
            fileSystemType = determineFileSystemTypeBasedOnIdentity(cloudStorageRequest.getIdentities());

            if (fileSystemType == null) {
                fileSystemType = determineFileSystemTypeBasedOnLocations(cloudStorageRequest.getLocations());
            }
        }
        return fileSystemType;
    }

    private FileSystemType determineFileSystemTypeBasedOnIdentity(List<StorageIdentityBase> identities) {
        FileSystemType fileSystemType = null;

        if (identities != null && !identities.isEmpty()) {

            StorageIdentityBase storageIdentityRequest = identities.get(0);

            if (storageIdentityRequest.getAdls() != null) {
                fileSystemType = storageIdentityRequest.getAdls().getType();
            } else if (storageIdentityRequest.getGcs() != null) {
                fileSystemType = storageIdentityRequest.getGcs().getType();
            } else if (storageIdentityRequest.getWasb() != null) {
                fileSystemType = storageIdentityRequest.getWasb().getType();
            } else if (storageIdentityRequest.getS3() != null) {
                fileSystemType = storageIdentityRequest.getS3().getType();
            } else if (storageIdentityRequest.getAdlsGen2() != null) {
                fileSystemType = storageIdentityRequest.getAdlsGen2().getType();
            }
        }

        return fileSystemType;
    }

    private FileSystemType determineFileSystemTypeBasedOnLocations(List<StorageLocationBase> locations) {
        FileSystemType fileSystemType = null;
        if (locations != null) {
            for (StorageLocationBase location : locations) {
                for (FileSystemType probableFsTye : FileSystemType.values()) {
                    if (locationStartsWith(location.getValue(), probableFsTye)) {
                        fileSystemType = probableFsTye;
                        break;
                    }
                }
            }
        }
        return fileSystemType;
    }

    private boolean locationStartsWith(String path, FileSystemType probableFsTye) {
        return path != null && path.startsWith(probableFsTye.getProtocol());
    }
}