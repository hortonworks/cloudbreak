package com.sequenceiq.cloudbreak.cloud.aws.context;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

public abstract class AwsStatusCheckerContext {

    private final AwsCredentialView awsCredentialView;

    protected AwsStatusCheckerContext(AwsCredentialView awsCredentialView) {
        this.awsCredentialView = awsCredentialView;
    }

    public AwsCredentialView getAwsCredentialView() {
        return awsCredentialView;
    }
}
