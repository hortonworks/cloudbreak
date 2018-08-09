package com.sequenceiq.cloudbreak.cloud.azure.context;

import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

public class AzureInteractiveLoginStatusCheckerContext {

    private Boolean cancelled = false;

    private final String deviceCode;

    private final CredentialNotifier credentialNotifier;

    private final ExtendedCloudCredential extendedCloudCredential;

    private final IdentityUser identityUser;

    public AzureInteractiveLoginStatusCheckerContext(String deviceCode, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier, IdentityUser identityUser) {
        this.deviceCode = deviceCode;
        this.extendedCloudCredential = extendedCloudCredential;
        this.credentialNotifier = credentialNotifier;
        this.identityUser = identityUser;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }

    public CredentialNotifier getCredentialNotifier() {
        return credentialNotifier;
    }

    public IdentityUser getIdentityUser() {
        return identityUser;
    }
}
