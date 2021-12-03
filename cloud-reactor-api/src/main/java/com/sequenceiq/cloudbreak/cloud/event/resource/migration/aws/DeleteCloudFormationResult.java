package com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class DeleteCloudFormationResult extends CloudPlatformResult {

    private boolean cloudFormationTemplateDeleted;

    public DeleteCloudFormationResult(Long resourceId, boolean cloudFormationTemplateDeleted) {
        super(resourceId);
        this.cloudFormationTemplateDeleted = cloudFormationTemplateDeleted;
    }

    public DeleteCloudFormationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public boolean isCloudFormationTemplateDeleted() {
        return cloudFormationTemplateDeleted;
    }
}
