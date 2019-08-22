package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.model.CloudIdentityType;

@Component
public class CloudFileSystemViewBuilder {

    @Inject
    private FileSystemConverter fileSystemConverter;

    public Optional<CloudFileSystemView> build(FileSystem fileSystem, Map<String, Set<String>> componentsByHostGroup, InstanceGroup instanceGroup) {
        Optional<CloudFileSystemView> fileSystemView;
        if (fileSystem != null) {
            SpiFileSystem spiFileSystem = fileSystemConverter.fileSystemToSpi(fileSystem);
            Set<String> components = componentsByHostGroup.get(instanceGroup.getGroupName());
            if (components != null && components.contains(KnoxRoles.IDBROKER)) {
                fileSystemView = spiFileSystem.getCloudFileSystems().stream()
                        .filter(cloudFileSystemView -> CloudIdentityType.ID_BROKER.equals(cloudFileSystemView.getCloudIdentityType()))
                        .findFirst();
            } else {
                fileSystemView = spiFileSystem.getCloudFileSystems().stream()
                        .filter(cloudFileSystemView -> CloudIdentityType.LOG.equals(cloudFileSystemView.getCloudIdentityType()))
                        .findFirst();
            }
        } else {
            fileSystemView = Optional.empty();
        }
        return fileSystemView;
    }
}
