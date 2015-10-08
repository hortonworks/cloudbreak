package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;

@Component
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureMetadataSetup implements MetadataSetup {
    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "collectMetadata"));
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, String instanceGroup, Integer scalingAdjustment) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "collectNewMetadata"));
    }

    @Override
    public InstanceSyncState getState(Stack stack, InstanceGroup instanceGroup, String instanceId) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "getState"));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public ResourceType getInstanceResourceType() {
        return ResourceType.AZURE_VIRTUAL_MACHINE;
    }
}
