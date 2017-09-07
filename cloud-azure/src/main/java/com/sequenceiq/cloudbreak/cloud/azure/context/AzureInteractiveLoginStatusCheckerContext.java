package com.sequenceiq.cloudbreak.cloud.azure.context;

import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class AzureInteractiveLoginStatusCheckerContext {

    private Boolean cancelled = false;

    private final String deviceCode;

    private final CredentialNotifier credentialNotifier;

    private final ExtendedCloudCredential extendedCloudCredential;

    public AzureInteractiveLoginStatusCheckerContext(String deviceCode, ExtendedCloudCredential extendedCloudCredential, CredentialNotifier credentialNotifier) {
        this.deviceCode = deviceCode;
        this.extendedCloudCredential = extendedCloudCredential;
        this.credentialNotifier = credentialNotifier;
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
}
