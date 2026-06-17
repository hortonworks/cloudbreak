package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class CredentialExperiencePolicyRequest extends CloudPlatformRequest<CredentialExperiencePolicyResult> {

    public CredentialExperiencePolicyRequest(CloudContext cloudContext) {
        super(cloudContext, null);
    }

    @Override
    public String toString() {
        return "CredentialExperiencePolicyRequest{} " + super.toString();
    }
}
