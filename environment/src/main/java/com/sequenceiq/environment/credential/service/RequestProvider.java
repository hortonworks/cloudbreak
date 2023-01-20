package com.sequenceiq.environment.credential.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CDPServicePolicyVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;

@Component
public class RequestProvider {

    public ResourceDefinitionRequest getResourceDefinitionRequest(CloudPlatformVariant platform, String resource) {
        return new ResourceDefinitionRequest(platform, resource);
    }

    public CredentialVerificationRequest getCredentialVerificationRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        return new CredentialVerificationRequest(cloudContext, cloudCredential);
    }

    public CredentialVerificationRequest getCredentialVerificationRequest(CloudContext cloudContext, CloudCredential cloudCredential,
            boolean creationVerification) {
        return new CredentialVerificationRequest(cloudContext, cloudCredential, creationVerification);
    }

    public CDPServicePolicyVerificationRequest getCDPServicePolicyVerificationRequest(CloudContext cloudContext, CloudCredential cloudCredential,
            List<String> services, Map<String, String> experiencePrerequisites) {
        return new CDPServicePolicyVerificationRequest(cloudContext, cloudCredential, services, experiencePrerequisites);
    }

}
