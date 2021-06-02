package com.sequenceiq.cloudbreak.cloud.aws.common.view;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class AwsCredentialViewProvider {

    private AwsCredentialViewProvider() {
    }

    public AwsCredentialView createAwsCredentialView(CloudCredential credential) {
        return new AwsCredentialView(credential);
    }

}
