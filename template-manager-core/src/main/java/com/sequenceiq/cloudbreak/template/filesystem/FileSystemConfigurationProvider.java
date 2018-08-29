package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
public class FileSystemConfigurationProvider {

    @Inject
    private AzureFileSystemConfigProvider azureFileSystemConfigProvider;

    @Inject
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    public BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fs, Stack stack) throws IOException {
        Optional<Resource> resource = Optional.empty();
        if (CloudConstants.AZURE.equals(stack.getPlatformVariant())) {
            resource = Optional.of(stack.getResourceByType(ResourceType.ARM_TEMPLATE));
        }
        return fileSystemConfiguration(fs, stack.getId(), stack.getUuid(), stack.getCredential(), stack.getPlatformVariant(), resource);
    }

    public BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fs, StackV2Request request, Credential credential) throws IOException {
        Resource resource = new Resource(ResourceType.ARM_TEMPLATE, request.getGeneral().getName(), null);
        return fileSystemConfiguration(fs, 0L, "fake-uuid", credential, credential.cloudPlatform(), Optional.of(resource));
    }

    private BaseFileSystemConfigurationsView fileSystemConfiguration(FileSystem fs, Long stackId, String uuid, Credential credential,
            String platformVariant, Optional<Resource> resource) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfiguration = null;
        if (fs != null) {
            fileSystemConfiguration = fileSystemConfigurationsViewProvider.propagateConfigurationsView(fs);
            fileSystemConfiguration.setStorageContainer("cloudbreak" + stackId);
            if (CloudConstants.AZURE.equals(platformVariant) && credential != null) {
                fileSystemConfiguration = azureFileSystemConfigProvider.decorateFileSystemConfiguration(uuid, credential,
                        resource.orElse(null), fileSystemConfiguration);
            }
        }
        return fileSystemConfiguration;
    }
}
