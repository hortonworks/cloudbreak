package com.sequenceiq.cloudbreak.cloud.aws.context;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

public abstract class AwsStatusCheckerContext {
    private AwsCredentialView awsCredentialView;

    public AwsStatusCheckerContext(AwsCredentialView awsCredentialView) {
        this.awsCredentialView = awsCredentialView;
    }

    public AwsCredentialView getAwsCredentialView() {
        return awsCredentialView;
    }
}
