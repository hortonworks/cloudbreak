package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.CloudStorageCdpService;

@Service
public class FileSystemConfigurationProvider {

    @Inject
    private AzureFileSystemConfigProvider azureFileSystemConfigProvider;

    @Inject
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    public BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fileSystem, StackView stack,
            Function<ResourceType, Collection<Resource>> resourceFuction, Json credentialAttributes,
            ConfigQueryEntries configQueryEntries) throws IOException {
        Optional<Resource> resource = Optional.empty();
        if (CloudConstants.AZURE.equals(stack.getPlatformVariant())) {
            Collection<Resource> stackResources = resourceFuction.apply(ResourceType.ARM_TEMPLATE);
            resource = stackResources.stream().findFirst();
        }
        return fileSystemConfiguration(
                fileSystem,
                stack.getId(),
                stack.getUuid(),
                credentialAttributes,
                stack.getPlatformVariant(),
                resource,
                configQueryEntries);
    }

    public BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fileSystem, StackV4Request request,
            Json credentialAttributes, ConfigQueryEntries configQueryEntries) throws IOException {
        Resource resource = new Resource(ResourceType.ARM_TEMPLATE, request.getName(), null, null);
        return fileSystemConfiguration(fileSystem, 0L, "fake-uuid", credentialAttributes,
                request.getCloudPlatform().name(), Optional.of(resource), configQueryEntries);
    }

    private BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fileSystem, Long stackId, String uuid, Json credentialAttributes,
            String platformVariant, Optional<Resource> resource, ConfigQueryEntries configQueryEntries) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfiguration = null;
        if (fileSystem != null) {
            fileSystemConfiguration = fileSystemConfigurationsViewProvider.propagateConfigurationsView(fileSystem, configQueryEntries);
            if (fileSystemConfiguration != null) {
                fileSystemConfiguration.setStorageContainer("cloudbreak" + stackId);
                if (CloudConstants.AZURE.equals(platformVariant) && credentialAttributes != null) {
                    fileSystemConfiguration = azureFileSystemConfigProvider.decorateFileSystemConfiguration(uuid, credentialAttributes,
                            resource.orElse(null), fileSystemConfiguration);
                }
                Optional<StorageLocation> remoteFs = Optional.ofNullable(fileSystem.getCloudStorage())
                        .map(CloudStorage::getLocations)
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(location -> location.getType().equals(CloudStorageCdpService.REMOTE_FS))
                        .findFirst();
                if (remoteFs.isPresent()) {
                    com.sequenceiq.cloudbreak.domain.StorageLocation storageLocation = new com.sequenceiq.cloudbreak.domain.StorageLocation();
                    storageLocation.setProperty(CloudStorageCdpService.REMOTE_FS.name());
                    storageLocation.setValue(remoteFs.get().getValue());
                    fileSystemConfiguration.getLocations().add(new StorageLocationView(storageLocation));
                }
            }
        }
        return fileSystemConfiguration;
    }
}
