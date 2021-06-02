package com.sequenceiq.cloudbreak.cloud.aws.common.view;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

public class AuthenticatedContextView {
    private AuthenticatedContext authenticatedContext;

    public AuthenticatedContextView(AuthenticatedContext authenticatedContext) {
        this.authenticatedContext = authenticatedContext;
    }

    public AmazonEc2Client getAmazonEC2Client() {
        return authenticatedContext.getParameter(AmazonEc2Client.class);
    }

    public String getRegion() {
        if (authenticatedContext == null ||
                authenticatedContext.getCloudContext() == null ||
                authenticatedContext.getCloudContext().getLocation() == null ||
                authenticatedContext.getCloudContext().getLocation().getRegion() == null) {
            return null;
        }
        return authenticatedContext.getCloudContext().getLocation().getRegion().value();
    }

    public AwsCredentialView getAwsCredentialView() {
        return new AwsCredentialView(authenticatedContext.getCloudCredential());
    }
}
