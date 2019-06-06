package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class ResourceDefinitionResult extends CloudPlatformResult {

    private final String definition;

    public ResourceDefinitionResult(Long resourceId, String definition) {
        super(resourceId);
        this.definition = definition;
    }

    public ResourceDefinitionResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        definition = null;
    }

    public String getDefinition() {
        return definition;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResourceDefinitionResult{");
        sb.append("definition='").append(JsonUtil.minify(definition)).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
