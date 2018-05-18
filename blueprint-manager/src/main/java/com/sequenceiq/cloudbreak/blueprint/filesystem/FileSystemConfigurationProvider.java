package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class FileSystemConfigurationProvider {

    @Inject
    private AzureFileSystemConfigProvider azureFileSystemConfigProvider;

    public FileSystemConfiguration fileSystemConfiguration(FileSystem fs, Stack stack) throws IOException {
        FileSystemConfiguration fileSystemConfiguration = null;
        if (fs != null) {
            FileSystemType fileSystemType = FileSystemType.valueOf(fs.getType());
            String json = JsonUtil.writeValueAsString(fs.getProperties());
            fileSystemConfiguration = JsonUtil.readValue(json, fileSystemType.getClazz());
            if (stack != null) {
                fileSystemConfiguration = decorateFsConfigurationProperties(fileSystemConfiguration, stack);
            }
        }
        return fileSystemConfiguration;
    }

    private FileSystemConfiguration decorateFsConfigurationProperties(FileSystemConfiguration fsConfiguration, Stack stack) {
        fsConfiguration.addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stack.getId());

        if (CloudConstants.AZURE.equals(stack.getPlatformVariant())) {
            com.sequenceiq.cloudbreak.domain.Resource resourceByType = stack.getResourceByType(ResourceType.ARM_TEMPLATE);
            fsConfiguration = azureFileSystemConfigProvider.decorateFileSystemConfiguration(stack.getUuid(), stack.getCredential(),
                    stack.getCluster().getFileSystem(), resourceByType, fsConfiguration);
        }
        return fsConfiguration;
    }
}
