package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.common.model.CloudIdentityType;

@Component
public class CloudFileSystemViewProvider {

    @Inject
    private FileSystemConverter fileSystemConverter;

    @Inject
    private CloudIdentityTypeDecider cloudIdentityTypeDecider;

    @Inject
    private InstanceGroupService instanceGroupService;

    public Optional<CloudFileSystemView> getCloudFileSystemView(FileSystem fileSystem,
            Map<String, Set<String>> componentsByHostGroup, InstanceGroup instanceGroup) {
        Optional<CloudFileSystemView> fileSystemView;
        if (fileSystem != null) {
            SpiFileSystem spiFileSystem = fileSystemConverter.fileSystemToSpi(fileSystem);
            Set<String> components = componentsByHostGroup.get(instanceGroup.getGroupName());
            CloudIdentityType identityType = cloudIdentityTypeDecider.getIdentityType(components);
            if (identityType == CloudIdentityType.ID_BROKER) {
                instanceGroupService.setCloudIdentityType(instanceGroup, CloudIdentityType.ID_BROKER);
                fileSystemView = spiFileSystem.getCloudFileSystems().stream()
                        .filter(cloudFileSystemView -> CloudIdentityType.ID_BROKER.equals(cloudFileSystemView.getCloudIdentityType()))
                        .findFirst();
            } else {
                instanceGroupService.setCloudIdentityType(instanceGroup, CloudIdentityType.LOG);
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
