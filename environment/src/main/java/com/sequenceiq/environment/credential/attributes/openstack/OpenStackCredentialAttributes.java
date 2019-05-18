package com.sequenceiq.environment.credential.attributes.openstack;

public class OpenStackCredentialAttributes {

    private String endpoint;

    private String facing;

    private String password;

    private String userName;

    private KeystoneV2Attributes keystoneV2;

    private KeystoneV3Attributes keystoneV3;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getFacing() {
        return facing;
    }

    public void setFacing(String facing) {
        this.facing = facing;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public KeystoneV2Attributes getKeystoneV2() {
        return keystoneV2;
    }

    public void setKeystoneV2(KeystoneV2Attributes keystoneV2) {
        this.keystoneV2 = keystoneV2;
    }

    public KeystoneV3Attributes getKeystoneV3() {
        return keystoneV3;
    }

    public void setKeystoneV3(KeystoneV3Attributes v3Parameter) {
        keystoneV3 = v3Parameter;
    }
}
