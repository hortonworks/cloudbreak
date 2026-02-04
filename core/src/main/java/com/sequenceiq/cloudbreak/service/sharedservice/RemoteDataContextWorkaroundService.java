package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;
import static com.sequenceiq.common.model.CloudStorageCdpService.DEFAULT_FS;
import static com.sequenceiq.common.model.CloudStorageCdpService.REMOTE_FS;
import static java.util.stream.Collectors.toList;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxFileSystemView;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@Service
public class RemoteDataContextWorkaroundService {

    @Inject
    private ResourceNameGenerator nameGenerator;

    public FileSystem prepareFilesystem(Cluster requestedCluster, SdxFileSystemView sdxFileSystemView, String datalakeCRN) {
        prepareFilesystemIfNotPresentedButSdxHasIt(requestedCluster, sdxFileSystemView);
        if (hasFilesystem(requestedCluster.getFileSystem())) {
            FileSystem datahubFileSystem = requestedCluster.getFileSystem();
            if (hasLocations(datahubFileSystem)) {
                CloudStorage datahubCloudStorage = datahubFileSystem.getCloudStorage();
                List<StorageLocation> hiveRelatedStorageConfigs = sdxFileSystemView.sharedFileSystemLocationsByService().entrySet()
                        .stream()
                        .map(entry -> {
                            StorageLocation location = new StorageLocation();
                            location.setType(CloudStorageCdpService.valueOf(entry.getKey()));
                            location.setValue(entry.getValue());
                            modifyLocationIfNeeded(location);
                            return location;
                        })
                        .toList();
                if (sdxHasHiveStorageConfiguration(hiveRelatedStorageConfigs)) {
                    List<StorageLocation> storageLocations = getStorageLocationsWithoutHiveRelatedLocations(datahubCloudStorage.getLocations());
                    storageLocations.addAll(hiveRelatedStorageConfigs);
                    datahubCloudStorage.setLocations(storageLocations);
                    datahubFileSystem.setCloudStorage(datahubCloudStorage);
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

    private void modifyLocationIfNeeded(StorageLocation location) {
        if (location.getType().equals(DEFAULT_FS)) {
            // set the datalake's default FS as the remote FS of the datahub
            location.setType(REMOTE_FS);
        }
    }

    private boolean hasLocations(FileSystem fileSystem) {
        return fileSystem.getCloudStorage() != null && fileSystem.getCloudStorage().getLocations() != null;
    }

    private boolean hasFilesystem(FileSystem fileSystem) {
        return fileSystem != null;
    }

    private void prepareFilesystemIfNotPresentedButSdxHasIt(Cluster requestedCluster, SdxFileSystemView sdxFileSystemView) {
        if (requestedCluster.getFileSystem() == null) {
            FileSystem fileSystem = new FileSystem();
            fileSystem.setCloudStorage(new CloudStorage());
            fileSystem.setName(nameGenerator.generateName(FILESYSTEM));
            fileSystem.setType(FileSystemType.valueOf(sdxFileSystemView.fileSystemType()));
            requestedCluster.setFileSystem(fileSystem);
        }
    }
}