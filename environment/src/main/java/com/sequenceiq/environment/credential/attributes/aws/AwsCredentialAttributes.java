package com.sequenceiq.environment.credential.attributes.aws;

public class AwsCredentialAttributes {

    private KeyBasedCredentialAttributes keyBased;

    private RoleBasedCredentialAttributes roleBased;

    private String defaultRegion;

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

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }
}
