package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(parent = KeystoneV3Base.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DomainKeystoneV3Parameters extends KeystoneV3Base {

    @NotNull
    @ApiModelProperty(required = true)
    private String domainName;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "DomainKeystoneV3Parameters{" +
                "domainName='" + domainName + '\'' +
                '}';
    }
}
