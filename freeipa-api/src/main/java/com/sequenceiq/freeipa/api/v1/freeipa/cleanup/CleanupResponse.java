package com.sequenceiq.freeipa.api.v1.freeipa.cleanup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("CleanupV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CleanupResponse {
    private Set<String> userCleanupSuccess = new HashSet<>();

    private Set<String> hostCleanupSuccess = new HashSet<>();

    private Set<String> roleCleanupSuccess = new HashSet<>();

    private Map<String, String> userCleanupFailed = new HashMap<>();

    private Map<String, String> hostCleanupFailed = new HashMap<>();

    private Map<String, String> roleCleanupFailed = new HashMap<>();

    public Set<String> getUserCleanupSuccess() {
        return userCleanupSuccess;
    }

    public void setUserCleanupSuccess(Set<String> userCleanupSuccess) {
        this.userCleanupSuccess = userCleanupSuccess;
    }

    public Set<String> getHostCleanupSuccess() {
        return hostCleanupSuccess;
    }

    public void setHostCleanupSuccess(Set<String> hostCleanupSuccess) {
        this.hostCleanupSuccess = hostCleanupSuccess;
    }

    public Map<String, String> getUserCleanupFailed() {
        return userCleanupFailed;
    }

    public void setUserCleanupFailed(Map<String, String> userCleanupFailed) {
        this.userCleanupFailed = userCleanupFailed;
    }

    public Map<String, String> getHostCleanupFailed() {
        return hostCleanupFailed;
    }

    public void setHostCleanupFailed(Map<String, String> hostCleanupFailed) {
        this.hostCleanupFailed = hostCleanupFailed;
    }

    public Set<String> getRoleCleanupSuccess() {
        return roleCleanupSuccess;
    }

    public void setRoleCleanupSuccess(Set<String> roleCleanupSuccess) {
        this.roleCleanupSuccess = roleCleanupSuccess;
    }

    public Map<String, String> getRoleCleanupFailed() {
        return roleCleanupFailed;
    }

    public void setRoleCleanupFailed(Map<String, String> roleCleanupFailed) {
        this.roleCleanupFailed = roleCleanupFailed;
    }
}
