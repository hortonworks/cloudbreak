package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AwsAuthenticationView {

    private final CloudCredential cloudCredential;

    public AwsAuthenticationView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getName() {
        return cloudCredential.getName();
    }

    public String getRoleArn() {
        return cloudCredential.getParameter("roleArn", String.class);
    }

    public String getAccessKey() {
        return cloudCredential.getParameter("accessKey", String.class);
    }

    public String getSecretKey() {
        return cloudCredential.getParameter("secretKey", String.class);
    }

    public String getCredentialCrn() {
        return cloudCredential.getId();
    }

}
