package com.sequenceiq.environment.environment.dto.credential;

import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.MOCK;
import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.UNKNOWN;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;

@Component
public class MockCredentialDetailsConverter implements CloudPlatformAwareCredentialDetailsConverter {
    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

    @Override
    public CredentialDetails.Builder convertCredentialDetails(CredentialAttributes credentialAttributes, CredentialDetails.Builder builder) {
        return builder.withCredentialType(credentialAttributes.getMock() == null ? UNKNOWN : MOCK);
    }
}
