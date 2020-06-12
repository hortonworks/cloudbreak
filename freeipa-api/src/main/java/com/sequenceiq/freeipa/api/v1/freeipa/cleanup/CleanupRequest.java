package com.sequenceiq.freeipa.api.v1.freeipa.cleanup;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CleanupV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CleanupRequest {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @ResourceObjectField(action = AuthorizationResourceAction.EDIT_ENVIRONMENT, variableType = AuthorizationVariableType.CRN)
    private String environmentCrn;

    private String clusterName;

    private Set<String> users;

    private Set<String> hosts;

    private Set<String> roles;

    private Set<String> ips;

    private Set<CleanupStep> cleanupStepsToSkip;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public void setHosts(Set<String> hosts) {
        this.hosts = hosts;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getIps() {
        return ips;
    }

    public void setIps(Set<String> ips) {
        this.ips = ips;
    }

    public Set<CleanupStep> getCleanupStepsToSkip() {
        return cleanupStepsToSkip;
    }

    public void setCleanupStepsToSkip(Set<CleanupStep> cleanupStepsToSkip) {
        this.cleanupStepsToSkip = cleanupStepsToSkip;
    }

    @Override
    public String toString() {
        return "CleanupRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", users=" + users +
                ", hosts=" + hosts +
                ", roles=" + roles +
                ", ips=" + ips +
                ", statesToSkip=" + cleanupStepsToSkip +
                '}';
    }
}
