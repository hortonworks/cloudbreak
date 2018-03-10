package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class GetPlatformTemplateResult extends CloudPlatformResult<CloudPlatformRequest<?>> {
    private String template;

    public GetPlatformTemplateResult(CloudPlatformRequest<?> request, String template) {
        super(request);
        this.template = template;
    }

    public GetPlatformTemplateResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public String getTemplate() {
        return template;
    }
}
