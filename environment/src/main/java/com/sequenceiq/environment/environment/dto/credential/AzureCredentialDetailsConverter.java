package com.sequenceiq.environment.environment.dto.credential;

import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.AZURE_APPBASED_CERTIFICATE;
import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.AZURE_APPBASED_SECRET;
import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.AZURE_CODEGRANTFLOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.UNKNOWN;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;

@Component
public class AzureCredentialDetailsConverter implements CloudPlatformAwareCredentialDetailsConverter {
    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public CredentialDetails.Builder convertCredentialDetails(CredentialAttributes credentialAttributes, CredentialDetails.Builder builder) {
        return builder.withCredentialType(getAzureCredentialType(credentialAttributes.getAzure()));
    }

    private CredentialType getAzureCredentialType(AzureCredentialAttributes azureCredentialAttributes) {
        CredentialType credentialType = UNKNOWN;
        if (azureCredentialAttributes != null) {
            if (azureCredentialAttributes.getCodeGrantFlowBased() != null) {
                credentialType = AZURE_CODEGRANTFLOW;
            } else if (azureCredentialAttributes.getAppBased() != null && azureCredentialAttributes.getAppBased().getAuthenticationType() != null) {
                switch (azureCredentialAttributes.getAppBased().getAuthenticationType()) {
                    case SECRET:
                        credentialType = AZURE_APPBASED_SECRET;
                        break;
                    case CERTIFICATE:
                        credentialType = AZURE_APPBASED_CERTIFICATE;
                        break;
                    default:
                        credentialType = UNKNOWN;
                        break;
                }
            }
        }
        return credentialType;
    }
}
