package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AwsCredentialView {

    private final CloudCredential cloudCredential;

    public AwsCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getName() {
        return cloudCredential.getName();
    }

    public String getRoleArn() {
        return cloudCredential.getParameter("roleArn", String.class);
    }

    public String getExternalId() {
        return cloudCredential.getParameter("externalId", String.class);
    }

    public Boolean isGovernmentCloudEnabled() {
        Object ev = cloudCredential.getParameter("govCloud", Object.class);
        if (ev instanceof Boolean) {
            return (Boolean) ev;
        } else if (ev instanceof String) {
            return Boolean.parseBoolean((String) ev);
        }
        return false;
    }

    public String getAccessKey() {
        return cloudCredential.getParameter("accessKey", String.class);
    }

    public String getSecretKey() {
        return cloudCredential.getParameter("secretKey", String.class);
    }

    public Long getId() {
        return cloudCredential.getId();
    }

}
