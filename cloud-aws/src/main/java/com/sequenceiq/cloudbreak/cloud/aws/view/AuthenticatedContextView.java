package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

public class AuthenticatedContextView {
    private AuthenticatedContext authenticatedContext;

    public AuthenticatedContextView(AuthenticatedContext authenticatedContext) {
        this.authenticatedContext = authenticatedContext;
    }

    public AmazonEC2Client getAmazonEC2Client() {
        return authenticatedContext.getParameter(AmazonEC2Client.class);
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
