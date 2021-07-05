package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KeystoneV3Parameters implements Serializable {

    @ApiModelProperty
    private ProjectKeystoneV3Parameters project;

    @ApiModelProperty
    private DomainKeystoneV3Parameters domain;

    public ProjectKeystoneV3Parameters getProject() {
        return project;
    }

    public void setProject(ProjectKeystoneV3Parameters project) {
        this.project = project;
    }

    public DomainKeystoneV3Parameters getDomain() {
        return domain;
    }

    public void setDomain(DomainKeystoneV3Parameters domain) {
        this.domain = domain;
    }

    @Override
    public String toString() {
        return "KeystoneV3Parameters{" +
                "project=" + project +
                ", domain=" + domain +
                '}';
    }
}
