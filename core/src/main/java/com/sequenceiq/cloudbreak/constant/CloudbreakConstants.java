package com.sequenceiq.cloudbreak.constant;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISKSET;

import java.util.Map;

import com.sequenceiq.common.api.type.ResourceType;

public final class CloudbreakConstants {
    public static final Map<String, ResourceType> VOLUME_RESOURCE_TYPE_BY_PLATFORM = Map.of(
            AZURE.name(), AZURE_VOLUMESET,
            AWS.name(), AWS_VOLUMESET,
            GCP.name(), GCP_ATTACHED_DISKSET
    );

    private CloudbreakConstants() {
    }

    public static boolean isVolumeSetResourceForPlatform(String cloudPlatform, ResourceType resourceType) {
        ResourceType volumeSetType = VOLUME_RESOURCE_TYPE_BY_PLATFORM.get(cloudPlatform);
        return volumeSetType != null && volumeSetType.equals(resourceType);
    }
}
