package com.sequenceiq.cloudbreak.cloud.arm;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class ArmClient {

    private static final int MAX_LENGTH_OF_RESOURCE_NAME = 24;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        AzureRMClient azureRMClient = createAccess(cloudCredential);
        authenticatedContext.putParameter(AzureRMClient.class, azureRMClient);
        return authenticatedContext;
    }

    public AzureRMClient createAccess(CloudCredential credential) {
        ArmCredentialView armCredential = new ArmCredentialView(credential);
        return createAccess(armCredential);
    }

    public AzureRMClient createAccess(ArmCredentialView armCredential) {
        return new AzureRMClient(armCredential.getTenantId(), armCredential.getAccesKey(), armCredential.getSecretKey(), armCredential.getSubscriptionId());
    }

    public String getStorageName(CloudContext cloudContext) {
        String result = cloudContext.getStackName().toLowerCase().replaceAll("\\s+|-", "") + cloudContext.getStackId() + cloudContext.getCreated();
        if (result.length() > MAX_LENGTH_OF_RESOURCE_NAME) {
            return result.substring(result.length() - MAX_LENGTH_OF_RESOURCE_NAME, result.length());
        }
        return result;
    }
}
