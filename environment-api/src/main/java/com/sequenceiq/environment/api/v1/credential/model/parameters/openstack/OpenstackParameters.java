package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("OpenstackV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class OpenstackParameters implements Serializable {

    @ApiModelProperty(required = true)
    private String endpoint;

    @ApiModelProperty(required = true, allowableValues = "public, admin, internal")
    private String facing;

    @ApiModelProperty(required = true)
    private String password;

    @ApiModelProperty(required = true)
    private String userName;

    private KeystoneV2Parameters keystoneV2;

    private KeystoneV3Parameters keystoneV3;

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

    public KeystoneV2Parameters getKeystoneV2() {
        return keystoneV2;
    }

    public void setKeystoneV2(KeystoneV2Parameters keystoneV2) {
        this.keystoneV2 = keystoneV2;
    }

    public KeystoneV3Parameters getKeystoneV3() {
        return keystoneV3;
    }

    public void setKeystoneV3(KeystoneV3Parameters v3Parameter) {
        keystoneV3 = v3Parameter;
    }

    @Override
    public String toString() {
        return "OpenstackParameters{" +
                "endpoint='" + endpoint + '\'' +
                ", facing='" + facing + '\'' +
                ", userName='" + userName + '\'' +
                ", keystoneV2=" + keystoneV2 +
                ", keystoneV3=" + keystoneV3 +
                '}';
    }
}
