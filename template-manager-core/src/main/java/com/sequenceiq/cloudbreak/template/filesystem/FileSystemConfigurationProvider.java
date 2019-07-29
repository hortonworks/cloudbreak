package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class FileSystemConfigurationProvider {

    @Inject
    private AzureFileSystemConfigProvider azureFileSystemConfigProvider;

    @Inject
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    public BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fileSystem, Stack stack,
            Json credentialAttributes, ConfigQueryEntries configQueryEntries) throws IOException {
        Optional<Resource> resource = Optional.empty();
        if (CloudConstants.AZURE.equals(stack.getPlatformVariant())) {
            resource = Optional.of(stack.getResourceByType(ResourceType.ARM_TEMPLATE));
        }
        return fileSystemConfiguration(fileSystem, stack.getId(), stack.getUuid(), credentialAttributes,
                stack.getPlatformVariant(), resource, configQueryEntries);
    }

    public BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fileSystem, StackV4Request request,
            Json credentialAttributes, ConfigQueryEntries configQueryEntries) throws IOException {
        Resource resource = new Resource(ResourceType.ARM_TEMPLATE, request.getName(), null);
        return fileSystemConfiguration(fileSystem, 0L, "fake-uuid", credentialAttributes,
                request.getCloudPlatform().name(), Optional.of(resource), configQueryEntries);
    }

    private BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fileSystem, Long stackId, String uuid, Json credentialAttributes,
            String platformVariant, Optional<Resource> resource, ConfigQueryEntries configQueryEntries) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfiguration = null;
        if (fileSystem != null) {
            fileSystemConfiguration = fileSystemConfigurationsViewProvider.propagateConfigurationsView(fileSystem, configQueryEntries);
            fileSystemConfiguration.setStorageContainer("cloudbreak" + stackId);
            if (CloudConstants.AZURE.equals(platformVariant) && credentialAttributes != null) {
                fileSystemConfiguration = azureFileSystemConfigProvider.decorateFileSystemConfiguration(uuid, credentialAttributes,
                        resource.orElse(null), fileSystemConfiguration);
            }
        }
        return fileSystemConfiguration;
    }
}
