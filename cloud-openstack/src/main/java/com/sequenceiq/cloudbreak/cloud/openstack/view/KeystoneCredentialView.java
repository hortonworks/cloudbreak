package com.sequenceiq.cloudbreak.cloud.openstack.view;

import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class KeystoneCredentialView {
    private static final String CB_KEYPAIR_NAME = "cb-keypair-";

    private CloudCredential cloudCredential;

    public KeystoneCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getKeyPairName() {
        return CB_KEYPAIR_NAME + deleteWhitespace(getName().toLowerCase());
    }

    public String getName() {
        return cloudCredential.getName();
    }

    public String getUserName() {
        return cloudCredential.getParameter("userName", String.class);
    }

    public String getPassword() {
        return cloudCredential.getParameter("password", String.class);
    }

    public String getTenantName() {
        return cloudCredential.getParameter("tenantName", String.class);
    }

    public String getEndpoint() {
        return cloudCredential.getParameter("endpoint", String.class);
    }
}
