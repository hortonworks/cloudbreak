package com.sequenceiq.cloudbreak.cloud.event.credential;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class CDPServicePolicyVerificationRequest extends CloudPlatformRequest<CDPServicePolicyVerificationResult> {

    private final List<String> services;

    private final Map<String, String> experiencePrerequisites;

    public CDPServicePolicyVerificationRequest(CloudContext cloudContext, CloudCredential cloudCredential,
        List<String> services, Map<String, String> experiencePrerequisites) {
        super(cloudContext, cloudCredential);
        this.services = services;
        this.experiencePrerequisites = experiencePrerequisites;
    }

    public List<String> getServices() {
        return services;
    }

    public Map<String, String> getExperiencePrerequisites() {
        return experiencePrerequisites;
    }
}
