package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;
import static java.util.stream.Collectors.toList;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class RemoteDataContextWorkaroundService {

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    public FileSystem prepareFilesytem(Cluster requestedCluster, Stack datalakeStack) {
        prepareFilesystemIfNotPresentedButSdxHasIt(requestedCluster, datalakeStack);
        if (hasFilesystem(requestedCluster.getFileSystem())) {
            FileSystem fileSystem = requestedCluster.getFileSystem();
            if (hasLocations(fileSystem)) {
                CloudStorage cloudStorage = fileSystem.getCloudStorage();
                FileSystem dlFileSystem = datalakeStack.getCluster().getFileSystem();
                if (hasFilesystem(dlFileSystem)) {
                    if (hasLocations(dlFileSystem)) {
                        List<StorageLocation> dlStorageLocations = dlFileSystem.getCloudStorage().getLocations();
                        List<StorageLocation> hiveRelatedStorageConfigs = getHiveRelatedStorageConfigs(dlStorageLocations);
                        if (sdxHasHiveStorageConfiguration(hiveRelatedStorageConfigs)) {
                            List<StorageLocation> storageLocations = getStorageLocationsWithoutHiveRelatedLocations(cloudStorage.getLocations());
                            storageLocations.addAll(hiveRelatedStorageConfigs);
                            cloudStorage.setLocations(storageLocations);
                            fileSystem.setCloudStorage(cloudStorage);
                        }
                    }
                }
            }
        }
        return requestedCluster.getFileSystem();
    }

    private boolean sdxHasHiveStorageConfiguration(List<StorageLocation> storageLocations) {
        return !storageLocations.isEmpty();
    }

    private List<StorageLocation> getStorageLocationsWithoutHiveRelatedLocations(List<StorageLocation> dlStorageLocations) {
        return dlStorageLocations.stream()
                .filter(e -> !e.getType().name().startsWith("HIVE"))
                .collect(toList());
    }

    private List<StorageLocation> getHiveRelatedStorageConfigs(List<StorageLocation> dlStorageLocations) {
        return dlStorageLocations.stream()
                .filter(e -> e.getType().name().startsWith("HIVE"))
                .collect(toList());
    }

    private boolean hasLocations(FileSystem fileSystem) {
        return fileSystem.getCloudStorage() != null && fileSystem.getCloudStorage().getLocations() != null;
    }

    private boolean hasFilesystem(FileSystem fileSystem) {
        return fileSystem != null;
    }

    private void prepareFilesystemIfNotPresentedButSdxHasIt(Cluster requestedCluster, Stack stack) {
        if (requestedCluster.getFileSystem() == null) {
            if (hasFilesystem(stack.getCluster().getFileSystem())) {
                FileSystem fileSystem = new FileSystem();
                fileSystem.setCloudStorage(new CloudStorage());
                fileSystem.setName(nameGenerator.generateName(FILESYSTEM));
                fileSystem.setType(stack.getCluster().getFileSystem().getType());
                requestedCluster.setFileSystem(fileSystem);
            }
        }
    }
}
