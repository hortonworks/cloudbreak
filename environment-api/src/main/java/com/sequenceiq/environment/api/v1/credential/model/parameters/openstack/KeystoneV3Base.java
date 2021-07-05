package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {DomainKeystoneV3Parameters.class, ProjectKeystoneV3Parameters.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public abstract class KeystoneV3Base implements Serializable {

    @ApiModelProperty(required = true)
    private String userDomain;

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    @Override
    public String toString() {
        return "KeystoneV3Base{" +
                "userDomain='" + userDomain + '\'' +
                '}';
    }
}
