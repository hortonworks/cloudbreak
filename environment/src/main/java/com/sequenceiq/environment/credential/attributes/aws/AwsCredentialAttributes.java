package com.sequenceiq.environment.credential.attributes.aws;

public class AwsCredentialAttributes {

    private KeyBasedCredentialAttributes keyBased;

    private RoleBasedCredentialAttributes roleBased;

    public KeyBasedCredentialAttributes getKeyBased() {
        return keyBased;
    }

    public void setKeyBased(KeyBasedCredentialAttributes keyBased) {
        this.keyBased = keyBased;
    }

    public RoleBasedCredentialAttributes getRoleBased() {
        return roleBased;
    }

    public void setRoleBased(RoleBasedCredentialAttributes roleBased) {
        this.roleBased = roleBased;
    }
}
