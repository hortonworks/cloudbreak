package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(allOf = KeystoneV3Base.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Deprecated
public class ProjectKeystoneV3Parameters extends KeystoneV3Base {

    @NotNull
    @Schema(required = true)
    private String projectDomainName;

    @NotNull
    @Schema(required = true)
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
