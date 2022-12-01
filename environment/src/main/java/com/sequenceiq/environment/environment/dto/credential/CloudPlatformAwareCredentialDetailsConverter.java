package com.sequenceiq.environment.environment.dto.credential;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;

public interface CloudPlatformAwareCredentialDetailsConverter {
    CloudPlatform getCloudPlatform();

    CredentialDetails.Builder convertCredentialDetails(CredentialAttributes credentialAttributes, CredentialDetails.Builder builder);
}
