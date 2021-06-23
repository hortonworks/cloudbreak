package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class RemoteDataContextWorkaroundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDataContextWorkaroundService.class);

    @Inject
    private StackService stackService;

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    public Set<RDSConfig> prepareRdsConfigs(Cluster requestedCluster, DatalakeResources datalakeResources) {
        return prepareRdsConfigs(requestedCluster, datalakeResources.getRdsConfigs());
    }

    public Set<RDSConfig> prepareRdsConfigs(Cluster requestedCluster, Set<RDSConfig> rdsConfigs) {
        Set<RDSConfig> rdsConfigsWithoutHive = requestedCluster.getRdsConfigs();
        Set<RDSConfig> hiveDbfromSdx = rdsConfigs
                .stream()
                .filter(this::isActiveHiveMetastoreDatabase)
                .collect(toSet());
        if (isHivePresentedInSdx(hiveDbfromSdx)) {
            rdsConfigsWithoutHive = requestedCluster.getRdsConfigs()
                    .stream()
                    .filter(rdsConfig -> !isHiveMetastoreDatabase(rdsConfig))
                    .collect(toSet());
        }
        rdsConfigsWithoutHive.addAll(hiveDbfromSdx);
        return rdsConfigsWithoutHive;
    }

    private boolean isHivePresentedInSdx(Set<RDSConfig> rdsConfigs) {
        return !rdsConfigs.isEmpty();
    }

    private boolean isActiveHiveMetastoreDatabase(RDSConfig rdsConfig) {
        ResourceStatus status = rdsConfig.getStatus();
        return (DEFAULT.equals(status) || USER_MANAGED.equals(status)) && isHiveMetastoreDatabase(rdsConfig);
    }

    private boolean isHiveMetastoreDatabase(RDSConfig rdsConfig) {
        return DatabaseType.HIVE.name().equals(rdsConfig.getType());
    }

    public FileSystem prepareFilesytem(Cluster requestedCluster, DatalakeResources datalakeResources) {
        Stack stack = stackService.getById(datalakeResources.getDatalakeStackId());
        return prepareFilesytem(requestedCluster, stack);
    }

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
