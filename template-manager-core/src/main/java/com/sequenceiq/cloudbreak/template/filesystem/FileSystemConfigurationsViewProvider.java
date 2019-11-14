package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.adlsgen2.AdlsGen2FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.gcs.GcsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.wasb.WasbFileSystemConfigurationsView;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;
import com.sequenceiq.common.model.CloudIdentityType;

@Service
public class FileSystemConfigurationsViewProvider {

    public BaseFileSystemConfigurationsView propagateConfigurationsView(FileSystem source, ConfigQueryEntries configQueryEntries) throws IOException {
        Set<StorageLocationView> locations;
        try {
            if (source.getLocations() != null && source.getLocations().getValue() != null) {
                locations = getLegacyStorageLocations(source);
            } else {
                CloudStorage cloudStorage = source.getCloudStorage();
                if (cloudStorage != null) {
                    locations = cloudStorage.getLocations().stream()
                            .map(storageLocation -> storageLocationToView(storageLocation, configQueryEntries))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toSet());
                } else {
                    locations = new HashSet<>();
                }
            }
        } catch (IOException e) {
            locations = new HashSet<>();
        }
        return getBaseFileSystemConfigurationsView(source, locations);
    }

    private Set<StorageLocationView> getLegacyStorageLocations(FileSystem source) throws IOException {
        Set<StorageLocationView> locations = new HashSet<>();
        StorageLocations storageLocations = source.getLocations().get(StorageLocations.class);
        if (storageLocations != null) {
            for (StorageLocation location : storageLocations.getLocations()) {
                locations.add(new StorageLocationView(location));
            }
        }
        return locations;
    }

    private Optional<StorageLocationView> storageLocationToView(com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation storageLocation,
            ConfigQueryEntries configQueryEntries) {
        return configQueryEntries.getEntries().stream()
                .filter(configQueryEntry -> configQueryEntry.getType().equals(storageLocation.getType()))
                .findFirst()
                .map(configQueryEntry -> {
                    StorageLocation storage = new StorageLocation();
                    storage.setValue(storageLocation.getValue());
                    storage.setProperty(configQueryEntry.getPropertyName());
                    storage.setConfigFile(configQueryEntry.getPropertyFile());
                    return new StorageLocationView(storage);
                });
    }

    private BaseFileSystemConfigurationsView getBaseFileSystemConfigurationsView(FileSystem source, Set<StorageLocationView> locations) throws IOException {
        if (source.getConfigurations() != null && source.getConfigurations().getValue() != null) {
            return getLegacyConfigurations(source, locations);
        }
        CloudStorage cloudStorage = source.getCloudStorage();
        if (cloudStorage != null) {
            if (source.getType().isS3()) {
                return s3IdentityToConfigView(locations, cloudStorage);
            } else if (source.getType().isWasb()) {
                return wasbIdentityToConfigView(locations);
            } else if (source.getType().isAdlsGen2()) {
                return adlsGen2IdentityToConfigView(locations, cloudStorage);
            }
        }
        return new BaseFileSystemConfigurationsView(source.getType().name(), locations);
    }

    private BaseFileSystemConfigurationsView getLegacyConfigurations(FileSystem source, Set<StorageLocationView> locations) throws IOException {
        if (source.getType().isAdls()) {
            return new AdlsFileSystemConfigurationsView(source.getConfigurations().get(AdlsFileSystem.class), locations, false);
        } else if (source.getType().isGcs()) {
            return new GcsFileSystemConfigurationsView(source.getConfigurations().get(GcsFileSystem.class), locations, false);
        } else if (source.getType().isS3()) {
            return new S3FileSystemConfigurationsView(source.getConfigurations().get(S3FileSystem.class), locations, false);
        } else if (source.getType().isWasb()) {
            return new WasbFileSystemConfigurationsView(source.getConfigurations().get(WasbFileSystem.class), locations, false);
        } else if (source.getType().isAdlsGen2()) {
            return new AdlsGen2FileSystemConfigurationsView(source.getConfigurations().get(AdlsGen2FileSystem.class), locations, false);
        }
        return null;
    }

    private BaseFileSystemConfigurationsView s3IdentityToConfigView(Set<StorageLocationView> locations, CloudStorage cloudStorage) {
        S3FileSystem s3FileSystem = new S3FileSystem();
        s3FileSystem.setS3GuardDynamoTableName(cloudStorage.getS3GuardDynamoTableName());
        return new S3FileSystemConfigurationsView(s3FileSystem, locations, false);
    }

    private BaseFileSystemConfigurationsView wasbIdentityToConfigView(Set<StorageLocationView> locations) {
        WasbFileSystem wasbFileSystem = new WasbFileSystem();
        return new WasbFileSystemConfigurationsView(wasbFileSystem, locations, false);
    }

    private BaseFileSystemConfigurationsView adlsGen2IdentityToConfigView(Set<StorageLocationView> locations, CloudStorage cloudStorage) {
        AdlsGen2FileSystem adlsGen2FileSystem = new AdlsGen2FileSystem();
        if (Objects.nonNull(cloudStorage) && Objects.nonNull(cloudStorage.getCloudIdentities()) && cloudStorage.getCloudIdentities().size() > 0) {
            Optional<CloudIdentity> idBrokerIdentity = cloudStorage.getCloudIdentities().stream()
                    .filter(cloudIdentity -> cloudIdentity.getIdentityType().equals(CloudIdentityType.ID_BROKER))
                    .findFirst();
            if (idBrokerIdentity.isPresent()) {
                return new AdlsGen2FileSystemConfigurationsView(adlsGen2FileSystem, locations, false,
                        idBrokerIdentity.get().getAdlsGen2Identity().getManagedIdentity());
            }
        }
        return new AdlsGen2FileSystemConfigurationsView(adlsGen2FileSystem, locations, false);
    }
}
