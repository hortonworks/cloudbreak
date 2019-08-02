package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
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
        Set<RDSConfig> rdsConfigsWithoutHive = requestedCluster.getRdsConfigs();
        Set<RDSConfig> hiveDbfromSdx = datalakeResources.getRdsConfigs()
                .stream()
                .filter(rdsConfig -> isHiveMetastoreDatabaseWhichIsDefault(rdsConfig))
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

    private boolean isHiveMetastoreDatabaseWhichIsDefault(RDSConfig rdsConfig) {
        return ResourceStatus.DEFAULT.equals(rdsConfig.getStatus()) && isHiveMetastoreDatabase(rdsConfig);
    }

    private boolean isHiveMetastoreDatabase(RDSConfig rdsConfig) {
        return DatabaseType.HIVE.name().equals(rdsConfig.getType());
    }

    public FileSystem prepareFilesytem(Cluster requestedCluster, DatalakeResources datalakeResources) {
        Stack stack = stackService.getById(datalakeResources.getDatalakeStackId());
        prepareFilesystemIfNotPresentedButSdxHasIt(requestedCluster, stack);
        if (hasFilesystem(requestedCluster.getFileSystem())) {
            FileSystem fileSystem = requestedCluster.getFileSystem();
            if (hasLocations(fileSystem)) {
                try {
                    StorageLocations storageLocations = getStorageLocations(fileSystem);

                    FileSystem dlFileSystem = stack.getCluster().getFileSystem();
                    if (hasFilesystem(dlFileSystem)) {
                        if (hasLocations(dlFileSystem)) {
                            StorageLocations dlStorageLocations = getStorageLocations(dlFileSystem);
                            Set<StorageLocation> hiveRelatedStorageConfigs = getHiveRelatedStorageConfigs(dlStorageLocations);
                            if (sdxHasHiveStorageConfiguration(hiveRelatedStorageConfigs)) {
                                storageLocations.setLocations(getStorageLocationsWithoutHiveRelatedLocations(storageLocations));
                                storageLocations.getLocations().addAll(hiveRelatedStorageConfigs);
                            }
                        }
                    }
                    requestedCluster.getFileSystem().setLocations(new Json(storageLocations));
                } catch (IOException e) {
                    LOGGER.info("Parsing storage locations was unsuccesfull.");
                }
            }
        }
        return requestedCluster.getFileSystem();
    }

    public boolean sdxHasHiveStorageConfiguration(Set<StorageLocation> storageLocations) {
        return !storageLocations.isEmpty();
    }

    private StorageLocations getStorageLocations(FileSystem fileSystem) throws IOException {
        StorageLocations storageLocations = fileSystem.getLocations().get(StorageLocations.class);
        if (storageLocations == null) {
            storageLocations = new StorageLocations();
        }
        return storageLocations;
    }

    private Set<StorageLocation> getStorageLocationsWithoutHiveRelatedLocations(StorageLocations storageLocations) {
        return storageLocations.getLocations()
                .stream()
                .filter(e -> !e.getConfigFile().startsWith("hive"))
                .collect(toSet());
    }

    private Set<StorageLocation> getHiveRelatedStorageConfigs(StorageLocations dlStorageLocations) {
        return dlStorageLocations.getLocations()
                .stream()
                .filter(e -> e.getConfigFile().startsWith("hive"))
                .collect(toSet());
    }

    private boolean hasLocations(FileSystem fileSystem) {
        return fileSystem.getLocations() != null && fileSystem.getLocations().getValue() != null;
    }

    private boolean hasFilesystem(FileSystem fileSystem) {
        return fileSystem != null;
    }

    private void prepareFilesystemIfNotPresentedButSdxHasIt(Cluster requestedCluster, Stack stack) {
        if (requestedCluster.getFileSystem() == null) {
            if (hasFilesystem(stack.getCluster().getFileSystem())) {
                FileSystem fileSystem = new FileSystem();
                fileSystem.setLocations(new Json(new StorageLocations()));
                fileSystem.setName(nameGenerator.generateName(FILESYSTEM));
                fileSystem.setType(stack.getCluster().getFileSystem().getType());
                requestedCluster.setFileSystem(fileSystem);
            }
        }
    }
}
