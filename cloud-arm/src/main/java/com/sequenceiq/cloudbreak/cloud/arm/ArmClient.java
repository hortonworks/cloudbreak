package com.sequenceiq.cloudbreak.cloud.arm;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class ArmClient {

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        AzureRMClient azureRMClient = getClient(cloudCredential);
        authenticatedContext.putParameter(AzureRMClient.class, azureRMClient);
        return authenticatedContext;
    }

    public AzureRMClient getClient(CloudCredential credential) {
        ArmCredentialView armCredential = new ArmCredentialView(credential);
        return getClient(armCredential);
    }

    public AzureRMClient getClient(ArmCredentialView armCredential) {
        return new AzureRMClient(armCredential.getTenantId(), armCredential.getAccessKey(), armCredential.getSecretKey(), armCredential.getSubscriptionId());
    }


}
