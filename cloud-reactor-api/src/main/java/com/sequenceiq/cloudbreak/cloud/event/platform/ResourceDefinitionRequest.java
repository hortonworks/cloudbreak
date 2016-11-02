package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;

public class ResourceDefinitionRequest extends CloudPlatformRequest<ResourceDefinitionResult> {

    private final CloudPlatformVariant platform;

    private final String resource;

    public ResourceDefinitionRequest(CloudPlatformVariant platform, String resource) {
        super(null, null);
        this.platform = platform;
        this.resource = resource;
    }

    public CloudPlatformVariant getPlatform() {
        return platform;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetResourceDefinition{");
        sb.append("platform='").append(platform).append('\'');
        sb.append(", resource='").append(resource).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
