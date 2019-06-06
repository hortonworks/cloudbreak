package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class GetPlatformTemplateResult extends CloudPlatformResult {
    private String template;

    public GetPlatformTemplateResult(Long resourceId, String template) {
        super(resourceId);
        this.template = template;
    }

    public GetPlatformTemplateResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public String getTemplate() {
        return template;
    }
}
