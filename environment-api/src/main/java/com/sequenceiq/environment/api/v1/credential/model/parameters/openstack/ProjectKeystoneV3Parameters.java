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
public class ProjectKeystoneV3Parameters extends KeystoneV3Base {

    @NotNull
    @ApiModelProperty(required = true)
    private String projectDomainName;

    @NotNull
    @ApiModelProperty(required = true)
    private String projectName;

    public String getProjectDomainName() {
        return projectDomainName;
    }

    public void setProjectDomainName(String projectDomainName) {
        this.projectDomainName = projectDomainName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public String toString() {
        return "ProjectKeystoneV3Parameters{" +
                "projectDomainName='" + projectDomainName + '\'' +
                ", projectName='" + projectName + '\'' +
                '}';
    }
}
