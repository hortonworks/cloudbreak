package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
public class FileSystemConfigurationProvider {

    @Inject
    private AzureFileSystemConfigProvider azureFileSystemConfigProvider;

    @Inject
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    public BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fs, Stack stack, Json credentialAttributes) throws IOException {
        Optional<Resource> resource = Optional.empty();
        if (CloudConstants.AZURE.equals(stack.getPlatformVariant())) {
            resource = Optional.of(stack.getResourceByType(ResourceType.ARM_TEMPLATE));
        }
        return fileSystemConfiguration(fs, stack.getId(), stack.getUuid(), credentialAttributes, stack.getPlatformVariant(), resource);
    }

    public BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fs, StackV4Request request, Json credentialAttributes)
            throws IOException {
        Resource resource = new Resource(ResourceType.ARM_TEMPLATE, request.getName(), null);
        return fileSystemConfiguration(fs, 0L, "fake-uuid", credentialAttributes, request.getCloudPlatform().name(), Optional.of(resource));
    }

    private BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fs, Long stackId, String uuid, Json credentialAttributes,
            String platformVariant, Optional<Resource> resource) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfiguration = null;
        if (fs != null) {
            fileSystemConfiguration = fileSystemConfigurationsViewProvider.propagateConfigurationsView(fs);
            fileSystemConfiguration.setStorageContainer("cloudbreak" + stackId);
            if (CloudConstants.AZURE.equals(platformVariant) && credentialAttributes != null) {
                fileSystemConfiguration = azureFileSystemConfigProvider.decorateFileSystemConfiguration(uuid, credentialAttributes,
                        resource.orElse(null), fileSystemConfiguration);
            }
        }
        return fileSystemConfiguration;
    }
}
