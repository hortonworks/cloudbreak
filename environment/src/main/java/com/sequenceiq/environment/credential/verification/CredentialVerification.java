package com.sequenceiq.environment.credential.verification;

import com.sequenceiq.environment.credential.domain.Credential;

public class CredentialVerification {

    private Credential credential;

    private boolean changed;

    public CredentialVerification(Credential credential, boolean changed) {
        this.credential = credential;
        this.changed = changed;
    }

    public Credential getCredential() {
        return credential;
    }

    public boolean isChanged() {
        return changed;
    }

}
