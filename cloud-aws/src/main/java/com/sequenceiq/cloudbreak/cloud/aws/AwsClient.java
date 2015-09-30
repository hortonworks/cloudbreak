package com.sequenceiq.cloudbreak.cloud.aws;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class AwsClient {

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        //AzureRMClient azureRMClient = createAccess(cloudCredential);
        //authenticatedContext.putParameter(AzureRMClient.class, azureRMClient);
        return authenticatedContext;
    }

    public AwsClient createAccess(CloudCredential credential) {
        //ArmCredentialView armCredential = new ArmCredentialView(credential);
        return null;
    }

    public AwsClient createAccess(AwsCredentialView armCredential) {
        return null;
    }
}
