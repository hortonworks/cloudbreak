package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {DomainKeystoneV3Parameters.class, ProjectKeystoneV3Parameters.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class KeystoneV3Base {

    @ApiModelProperty(required = true)
    private String userDomain;

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

}
