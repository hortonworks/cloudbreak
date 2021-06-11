package com.sequenceiq.cloudbreak.cloud.aws.common.context;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

public abstract class AwsStatusCheckerContext {

    private final AwsCredentialView awsCredentialView;

    protected AwsStatusCheckerContext(AwsCredentialView awsCredentialView) {
        this.awsCredentialView = awsCredentialView;
    }

    public AwsCredentialView getAwsCredentialView() {
        return awsCredentialView;
    }
}
