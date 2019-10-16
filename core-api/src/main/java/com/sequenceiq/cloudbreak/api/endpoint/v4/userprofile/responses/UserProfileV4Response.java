package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;

@ApiModel
public class UserProfileV4Response {

    private String username;

    private String userId;

    private String tenant;

    private List<String> entitlements = new ArrayList<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public List<String> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(List<String> entitlements) {
        this.entitlements = entitlements;
    }
}
