package com.sequenceiq.freeipa.service.rebuild;

import java.nio.file.Paths;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class RebuildRequestValidator {

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void validate(RebuildV2Request request, Stack stack) {
        validateCrnMatches(request, stack);
        validateInstanceExistsInStack(request, stack);
        validateCloudStorageLocations(request, stack);
    }

    private void validateCrnMatches(RebuildV2Request request, Stack stack) {
        if (!request.getResourceCrn().equals(stack.getResourceCrn())) {
            throw new BadRequestException("Invalid resource crn: " + request.getResourceCrn());
        }
    }

    private void validateInstanceExistsInStack(RebuildV2Request request, Stack stack) {
        Set<InstanceMetaData> allInstancesForStack = instanceMetaDataService.findAllInstancesForStack(stack.getId());
        if (allInstancesForStack.stream().noneMatch(im -> im.getDiscoveryFQDN().equals(request.getInstanceToRestoreFqdn()))) {
            throw new BadRequestException("Instance does not exist: " + request.getInstanceToRestoreFqdn());
        }
    }

    private void validateCloudStorageLocations(RebuildV2Request request, Stack stack) {
        switch (CloudPlatform.valueOf(stack.getCloudPlatform())) {
            case AWS -> {
                validatePath(request.getFullBackupStorageLocation(), FileSystemType.S3);
                validatePath(request.getDataBackupStorageLocation(), FileSystemType.S3);
            }
            case GCP -> {
                validatePath(request.getFullBackupStorageLocation(), FileSystemType.GCS);
                validatePath(request.getDataBackupStorageLocation(), FileSystemType.GCS);
            }
            case AZURE -> {
                validatePath(request.getFullBackupStorageLocation(), FileSystemType.ADLS_GEN_2);
                validatePath(request.getDataBackupStorageLocation(), FileSystemType.ADLS_GEN_2);
            }
            default -> throw new BadRequestException("Cloudplatform not supported for rebuild: " + stack.getCloudPlatform());
        }
    }

    private void validatePath(String storageLocation, FileSystemType fileSystemType) {
        try {
            Paths.get(storageLocation);
        } catch (Exception e) {
            throw new BadRequestException("Invalid storage location path: [" + storageLocation + "]");
        }
        if (fileSystemType.getLoggingProtocol().stream().noneMatch(loggingProtocol -> storageLocation.startsWith(loggingProtocol + "://"))) {
            throw new BadRequestException("Invalid storage location path: [" + storageLocation +
                    "]. It must start with one of the protocols: " + fileSystemType.getLoggingProtocol().stream().sorted().toList());
        }
    }
}
