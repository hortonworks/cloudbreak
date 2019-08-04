package com.sequenceiq.cloudbreak.service.filesystem;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.common.api.filesystem.FileSystemType;

@Service
public class FileSystemResolver {

    public FileSystemType determineFileSystemType(CloudStorageV4Request source) {
        FileSystemType fileSystemType = null;

        if (source.getAdls() != null) {
            fileSystemType = source.getAdls().getType();
        } else if (source.getGcs() != null) {
            fileSystemType = source.getGcs().getType();
        } else if (source.getWasb() != null) {
            fileSystemType = source.getWasb().getType();
        } else if (source.getS3() != null) {
            fileSystemType = source.getS3().getType();
        } else if (source.getAdlsGen2() != null) {
            fileSystemType = source.getAdlsGen2().getType();
        }

        if (fileSystemType == null && source.getLocations() != null) {
            fileSystemType = determineFileSystemTypeBasedOnLocations(source.getLocations());

        }
        return fileSystemType;
    }

    private FileSystemType determineFileSystemTypeBasedOnLocations(Set<StorageLocationV4Request> locations) {
        FileSystemType fileSystemType = null;
        for (StorageLocationV4Request location : locations) {
            for (FileSystemType probableFsTye : FileSystemType.values()) {
                if (locationStartsWith(location.getValue(), probableFsTye)) {
                    fileSystemType = probableFsTye;
                    break;
                }
            }
        }
        return fileSystemType;
    }

    private boolean locationStartsWith(String path, FileSystemType probableFsTye) {
        return path != null && path.startsWith(probableFsTye.getProtocol());
    }
}
