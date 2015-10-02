package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AwsCredentialView {

    private CloudCredential cloudCredential;

    public AwsCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getPublicKey() {
        return cloudCredential.getPublicKey();
    }

    public String getName() {
        return cloudCredential.getName();
    }

    public String getRoleArn() {
        return cloudCredential.getParameter("roleArn", String.class);
    }

    public String getKeyPairName() {
        return cloudCredential.getParameter("keyPairName", String.class);
    }

    public String getAccessKeyId() {
        return cloudCredential.getParameter("accessKeyId", String.class);
    }

    public String getSecretAccessKey() {
        return cloudCredential.getParameter("secretAccessKey", String.class);
    }

    public String getSessionToken() {
        return cloudCredential.getParameter("sessionToken", String.class);
    }

    public String getValidUntil() {
        return cloudCredential.getParameter("validUntil", String.class);
    }

    public Long getId() {
        return cloudCredential.getId();
    }

}
