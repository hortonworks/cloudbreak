package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenstackCredentialV4Parameters implements CredentialV4Parameters {

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
        this.keystoneV3 = v3Parameter;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> mapOfFields = new LinkedHashMap<>();
        mapOfFields.put("endpoint", endpoint);
        mapOfFields.put("facing", facing);
        mapOfFields.put("password", password);
        mapOfFields.put("userName", userName);
        if (keystoneV2 != null) {
            mapOfFields.putAll(keystoneV2.asMap());
        } else if (keystoneV3 != null) {
            mapOfFields.putAll(keystoneV3.asMap());
        }
        return mapOfFields;
    }

}
