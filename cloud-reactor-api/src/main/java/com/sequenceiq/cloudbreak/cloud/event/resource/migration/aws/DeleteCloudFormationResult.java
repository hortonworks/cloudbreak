package com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class DeleteCloudFormationResult extends CloudPlatformResult implements FlowPayload {

    private final boolean cloudFormationTemplateDeleted;

    public DeleteCloudFormationResult(Long resourceId, boolean cloudFormationTemplateDeleted) {
        super(resourceId);
        this.cloudFormationTemplateDeleted = cloudFormationTemplateDeleted;
    }

    @JsonCreator
    public DeleteCloudFormationResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        this.cloudFormationTemplateDeleted = false;
    }

    public boolean isCloudFormationTemplateDeleted() {
        return cloudFormationTemplateDeleted;
    }
}
