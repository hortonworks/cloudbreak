package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OpenstackV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class OpenstackParameters implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String endpoint;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String facing;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;

    @Deprecated
    private KeystoneV2Parameters keystoneV2;

    private KeystoneV3Parameters keystoneV3;

    private String remoteEnvironmentCrn;

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

    @Deprecated
    public KeystoneV2Parameters getKeystoneV2() {
        return keystoneV2;
    }

    @Deprecated
    public void setKeystoneV2(KeystoneV2Parameters keystoneV2) {
        this.keystoneV2 = keystoneV2;
    }

    public KeystoneV3Parameters getKeystoneV3() {
        return keystoneV3;
    }

    public void setKeystoneV3(KeystoneV3Parameters v3Parameter) {
        keystoneV3 = v3Parameter;
    }

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public void setRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
    }

    @Override
    public String toString() {
        return "OpenstackParameters{" +
                "endpoint='" + endpoint + '\'' +
                ", facing='" + facing + '\'' +
                ", userName='" + userName + '\'' +
                ", keystoneV2=" + keystoneV2 +
                ", keystoneV3=" + keystoneV3 +
                ", remoteEnvironmentCrn='" + remoteEnvironmentCrn + '\'' +
                '}';
    }
}
