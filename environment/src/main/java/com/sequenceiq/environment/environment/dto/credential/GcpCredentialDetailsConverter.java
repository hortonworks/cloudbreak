package com.sequenceiq.environment.environment.dto.credential;

import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.GCP_JSON;
import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.GCP_P12;
import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.UNKNOWN;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;

@Component
public class GcpCredentialDetailsConverter implements CloudPlatformAwareCredentialDetailsConverter {
    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public CredentialDetails.Builder convertCredentialDetails(CredentialAttributes credentialAttributes, CredentialDetails.Builder builder) {
        return builder.withCredentialType(getGcpCredentialType(credentialAttributes.getGcp()));
    }

    private CredentialType getGcpCredentialType(GcpCredentialAttributes gcpCredentialAttributes) {
        CredentialType credentialType = UNKNOWN;
        if (gcpCredentialAttributes != null) {
            if (gcpCredentialAttributes.getJson() != null) {
                credentialType = GCP_JSON;
            } else if (gcpCredentialAttributes.getP12() != null) {
                credentialType = GCP_P12;
            }
        }
        return credentialType;
    }
}
